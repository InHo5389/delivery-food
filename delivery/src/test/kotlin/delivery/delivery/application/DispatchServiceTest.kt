package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.DispatchOffer
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.RiderCandidateRepository
import delivery.delivery.infrastructure.RiderCandidateRow
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatchServiceTest {

    private val deliveryRepository = mockk<DeliveryRepository>()
    private val riderCandidateRepository = mockk<RiderCandidateRepository>()
    private val dispatchOfferRepository = mockk<DispatchOfferRepository>()
    private lateinit var dispatchService: DispatchService

    @BeforeEach
    fun setUp() {
        dispatchService = DispatchService(deliveryRepository, riderCandidateRepository, dispatchOfferRepository)
        every { dispatchOfferRepository.save(any()) } answers { it.invocation.args[0] as DispatchOffer }
    }

    private fun candidateRow(riderId: Long) = RiderCandidateRow(riderId)

    @Test
    fun `존재하지 않는 배달을 배차하면 예외가 발생한다`() {
        every { deliveryRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { dispatchService.dispatchOne(999L) }

        assertEquals(DeliveryErrorCode.DELIVERY_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `후보가 없으면 오퍼를 보내지 않고 PENDING을 유지한다`() {
        val delivery = Delivery.withId(1L, orderId = 1L, shopId = 1L)
        every { deliveryRepository.findById(1L) } returns Optional.of(delivery)
        every { dispatchOfferRepository.findAllByDeliveryId(1L) } returns emptyList()
        every { riderCandidateRepository.findAvailableCandidates(any(), any(), any()) } returns emptyList()

        val actual = dispatchService.dispatchOne(1L)

        assertTrue(actual.offeredRiderIds.isEmpty())
        assertEquals(DeliveryStatus.PENDING, delivery.status)
        verify(exactly = 0) { dispatchOfferRepository.save(any()) }
    }

    @Test
    fun `후보가 있으면 오퍼를 보내고 OFFERING으로 전이한다`() {
        val delivery = Delivery.withId(1L, orderId = 1L, shopId = 1L)
        every { deliveryRepository.findById(1L) } returns Optional.of(delivery)
        every { dispatchOfferRepository.findAllByDeliveryId(1L) } returns emptyList()
        every { riderCandidateRepository.findAvailableCandidates(any(), any(), any()) } returns listOf(candidateRow(10L))

        val actual = dispatchService.dispatchOne(1L)

        assertEquals(listOf(10L), actual.offeredRiderIds)
        assertEquals(DeliveryStatus.OFFERING, delivery.status)
        verify(exactly = 1) { dispatchOfferRepository.save(any()) }
    }

    @Test
    fun `후보가 여러 명이면 전원에게 오퍼를 보낸다`() {
        val delivery = Delivery.withId(1L, orderId = 1L, shopId = 1L)
        every { deliveryRepository.findById(1L) } returns Optional.of(delivery)
        every { dispatchOfferRepository.findAllByDeliveryId(1L) } returns emptyList()
        every { riderCandidateRepository.findAvailableCandidates(any(), any(), any()) } returns listOf(
            candidateRow(1L),
            candidateRow(2L),
            candidateRow(3L),
            candidateRow(4L),
        )

        val actual = dispatchService.dispatchOne(1L)

        assertEquals(listOf(1L, 2L, 3L, 4L), actual.offeredRiderIds)
        verify(exactly = 4) { dispatchOfferRepository.save(any()) }
    }

    @Test
    fun `이미 오퍼를 받은 라이더는 다시 후보에 포함되지 않는다`() {
        val delivery = Delivery.withId(1L, orderId = 1L, shopId = 1L, status = DeliveryStatus.OFFERING)
        every { deliveryRepository.findById(1L) } returns Optional.of(delivery)
        every { dispatchOfferRepository.findAllByDeliveryId(1L) } returns listOf(
            DispatchOffer.withId(1L, deliveryId = 1L, riderId = 10L)
        )
        every { riderCandidateRepository.findAvailableCandidates(any(), any(), any()) } returns listOf(
            candidateRow(10L),
            candidateRow(20L),
        )
        val savedOffer = slot<DispatchOffer>()
        every { dispatchOfferRepository.save(capture(savedOffer)) } answers { it.invocation.args[0] as DispatchOffer }

        val actual = dispatchService.dispatchOne(1L)

        assertEquals(listOf(20L), actual.offeredRiderIds)
        assertEquals(20L, savedOffer.captured.riderId)
    }

    @Test
    fun `이미 오퍼를 받은 라이더뿐이면 추가 오퍼 없이 상태를 유지한다`() {
        val delivery = Delivery.withId(1L, orderId = 1L, shopId = 1L, status = DeliveryStatus.OFFERING)
        every { deliveryRepository.findById(1L) } returns Optional.of(delivery)
        every { dispatchOfferRepository.findAllByDeliveryId(1L) } returns listOf(
            DispatchOffer.withId(1L, deliveryId = 1L, riderId = 10L)
        )
        every { riderCandidateRepository.findAvailableCandidates(any(), any(), any()) } returns listOf(candidateRow(10L))

        val actual = dispatchService.dispatchOne(1L)

        assertTrue(actual.offeredRiderIds.isEmpty())
        assertEquals(DeliveryStatus.OFFERING, delivery.status)
        verify(exactly = 0) { dispatchOfferRepository.save(any()) }
    }

    @Test
    fun `한 사이클에서 한 배달이 실패해도 나머지 배달은 배차된다`() {
        val failing = Delivery.withId(1L, orderId = 1L, shopId = 1L)
        val succeeding = Delivery.withId(2L, orderId = 2L, shopId = 1L)
        every { deliveryRepository.findAllByStatus(DeliveryStatus.PENDING) } returns listOf(failing, succeeding)
        every { deliveryRepository.findById(1L) } throws RuntimeException("DB 커넥션 오류")
        every { deliveryRepository.findById(2L) } returns Optional.of(succeeding)
        every { dispatchOfferRepository.findAllByDeliveryId(2L) } returns emptyList()
        every { riderCandidateRepository.findAvailableCandidates(any(), any(), any()) } returns emptyList()

        val actual = dispatchService.runDispatchCycle()

        assertEquals(1, actual.size)
        assertEquals(2L, actual[0].deliveryId)
    }
}
