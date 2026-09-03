package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.dto.AcceptOfferCommand
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DispatchOffer
import delivery.delivery.domain.DispatchOfferStatus
import delivery.delivery.domain.Rider
import delivery.delivery.domain.RiderStatus
import delivery.delivery.infrastructure.DeliveryAssignmentRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.RiderRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.assertEquals

class DispatchOfferServiceTest {

    private val dispatchOfferRepository = mockk<DispatchOfferRepository>()
    private val riderRepository = mockk<RiderRepository>()
    private val deliveryAssignmentRepository = mockk<DeliveryAssignmentRepository>()
    private lateinit var dispatchOfferService: DispatchOfferService

    @BeforeEach
    fun setUp() {
        dispatchOfferService = DispatchOfferService(dispatchOfferRepository, riderRepository, deliveryAssignmentRepository)
    }

    private fun rider(id: Long = 1L, accountId: Long = 100L, status: RiderStatus = RiderStatus.AVAILABLE) =
        Rider.withId(id, accountId, status = status)

    @Test
    fun `존재하지 않는 오퍼를 수락하면 예외가 발생한다`() {
        every { dispatchOfferRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { dispatchOfferService.accept(AcceptOfferCommand(999L, 100L)) }

        assertEquals(DeliveryErrorCode.OFFER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `라이더 프로필이 없으면 예외가 발생한다`() {
        val offer = DispatchOffer.withId(1L, deliveryId = 1L, riderId = 1L)
        every { dispatchOfferRepository.findById(1L) } returns Optional.of(offer)
        every { riderRepository.findByAccountId(100L) } returns null

        val exception = assertThrows<BusinessException> { dispatchOfferService.accept(AcceptOfferCommand(1L, 100L)) }

        assertEquals(DeliveryErrorCode.RIDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `본인에게 온 오퍼가 아니면 예외가 발생한다`() {
        val offer = DispatchOffer.withId(1L, deliveryId = 1L, riderId = 2L)
        every { dispatchOfferRepository.findById(1L) } returns Optional.of(offer)
        every { riderRepository.findByAccountId(100L) } returns rider(id = 1L, accountId = 100L)

        val exception = assertThrows<BusinessException> { dispatchOfferService.accept(AcceptOfferCommand(1L, 100L)) }

        assertEquals(DeliveryErrorCode.NOT_YOUR_OFFER, exception.errorCode)
    }

    @Test
    fun `이미 다른 라이더가 배정된 배달이면 예외가 발생한다`() {
        val offer = DispatchOffer.withId(1L, deliveryId = 10L, riderId = 1L)
        every { dispatchOfferRepository.findById(1L) } returns Optional.of(offer)
        every { riderRepository.findByAccountId(100L) } returns rider(id = 1L, accountId = 100L)
        every { deliveryAssignmentRepository.tryAssignRider(10L, 1L) } returns false

        val exception = assertThrows<BusinessException> { dispatchOfferService.accept(AcceptOfferCommand(1L, 100L)) }

        assertEquals(DeliveryErrorCode.DISPATCH_ALREADY_ASSIGNED, exception.errorCode)
    }

    @Test
    fun `정상적으로 수락하면 오퍼 상태가 ACCEPTED로 바뀌고 라이더는 BUSY가 된다`() {
        val offer = DispatchOffer.withId(1L, deliveryId = 10L, riderId = 1L)
        val acceptingRider = rider(id = 1L, accountId = 100L)
        every { dispatchOfferRepository.findById(1L) } returns Optional.of(offer)
        every { riderRepository.findByAccountId(100L) } returns acceptingRider
        every { deliveryAssignmentRepository.tryAssignRider(10L, 1L) } returns true
        every { dispatchOfferRepository.findAllByDeliveryId(10L) } returns listOf(offer)

        val actual = dispatchOfferService.accept(AcceptOfferCommand(1L, 100L))

        assertEquals(DispatchOfferStatus.ACCEPTED, actual.status)
        assertEquals(RiderStatus.BUSY, acceptingRider.status)
    }

    @Test
    fun `수락되면 같은 배달의 다른 SENT 오퍼는 REJECTED로 바뀐다`() {
        val acceptedOffer = DispatchOffer.withId(1L, deliveryId = 10L, riderId = 1L)
        val otherOffer = DispatchOffer.withId(2L, deliveryId = 10L, riderId = 2L)
        every { dispatchOfferRepository.findById(1L) } returns Optional.of(acceptedOffer)
        every { riderRepository.findByAccountId(100L) } returns rider(id = 1L, accountId = 100L)
        every { deliveryAssignmentRepository.tryAssignRider(10L, 1L) } returns true
        every { dispatchOfferRepository.findAllByDeliveryId(10L) } returns listOf(acceptedOffer, otherOffer)

        dispatchOfferService.accept(AcceptOfferCommand(1L, 100L))

        assertEquals(DispatchOfferStatus.REJECTED, otherOffer.status)
    }

    @Test
    fun `이미 응답된 오퍼는 재수락 처리에서 건드리지 않는다`() {
        val acceptedOffer = DispatchOffer.withId(1L, deliveryId = 10L, riderId = 1L)
        val expiredOffer = DispatchOffer.withId(3L, deliveryId = 10L, riderId = 3L, status = DispatchOfferStatus.EXPIRED)
        every { dispatchOfferRepository.findById(1L) } returns Optional.of(acceptedOffer)
        every { riderRepository.findByAccountId(100L) } returns rider(id = 1L, accountId = 100L)
        every { deliveryAssignmentRepository.tryAssignRider(10L, 1L) } returns true
        every { dispatchOfferRepository.findAllByDeliveryId(10L) } returns listOf(acceptedOffer, expiredOffer)

        dispatchOfferService.accept(AcceptOfferCommand(1L, 100L))

        assertEquals(DispatchOfferStatus.EXPIRED, expiredOffer.status)
    }
}
