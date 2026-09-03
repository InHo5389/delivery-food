package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DispatchOffer
import delivery.delivery.domain.DispatchOfferStatus
import delivery.delivery.domain.Rider
import delivery.delivery.domain.RiderStatus
import delivery.delivery.infrastructure.DeliveryAssignmentRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.DispatchQueueRepository
import delivery.delivery.infrastructure.DispatchQueueRow
import delivery.delivery.infrastructure.RiderRepository
import delivery.order.application.OrderService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatchQueueServiceTest {

    private val dispatchQueueRepository = mockk<DispatchQueueRepository>()
    private val riderRepository = mockk<RiderRepository>()
    private val deliveryAssignmentRepository = mockk<DeliveryAssignmentRepository>()
    private val dispatchOfferRepository = mockk<DispatchOfferRepository>()
    private val orderService = mockk<OrderService>()
    private lateinit var dispatchQueueService: DispatchQueueService

    @BeforeEach
    fun setUp() {
        dispatchQueueService = DispatchQueueService(dispatchQueueRepository, riderRepository, deliveryAssignmentRepository, dispatchOfferRepository, orderService)
    }

    private fun availableRider(id: Long = 1L, accountId: Long = 100L) = Rider.withId(id, accountId, status = RiderStatus.AVAILABLE)

    @Test
    fun `큐가 비어 있으면 빈 목록을 반환한다`() {
        every { dispatchQueueRepository.findQueue(20) } returns emptyList()

        val actual = dispatchQueueService.getQueue(20)

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `조회 결과를 그대로 큐 항목으로 변환한다`() {
        every { dispatchQueueRepository.findQueue(20) } returns listOf(DispatchQueueRow(deliveryId = 1L, orderId = 10L, shopId = 100L, estimatedPickupAt = null))

        val actual = dispatchQueueService.getQueue(20)

        assertEquals(1, actual.size)
        assertEquals(1L, actual[0].deliveryId)
        assertEquals(10L, actual[0].orderId)
        assertEquals(100L, actual[0].shopId)
    }

    @Test
    fun `요청한 limit을 그대로 리포지토리에 전달한다`() {
        every { dispatchQueueRepository.findQueue(5) } returns emptyList()

        dispatchQueueService.getQueue(5)

        verify(exactly = 1) { dispatchQueueRepository.findQueue(5) }
    }

    @Test
    fun `라이더 프로필이 없으면 클레임에 실패한다`() {
        every { riderRepository.findByAccountId(100L) } returns null

        val exception = assertThrows<BusinessException> { dispatchQueueService.claim(100L) }

        assertEquals(DeliveryErrorCode.RIDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `AVAILABLE이 아닌 라이더는 클레임할 수 없다`() {
        every { riderRepository.findByAccountId(100L) } returns Rider.withId(1L, 100L, status = RiderStatus.BUSY)

        val exception = assertThrows<BusinessException> { dispatchQueueService.claim(100L) }

        assertEquals(DeliveryErrorCode.RIDER_NOT_AVAILABLE, exception.errorCode)
    }

    @Test
    fun `큐가 비어 있으면 클레임에 실패한다`() {
        every { riderRepository.findByAccountId(100L) } returns availableRider()
        every { dispatchQueueRepository.claimNext() } returns null

        val exception = assertThrows<BusinessException> { dispatchQueueService.claim(100L) }

        assertEquals(DeliveryErrorCode.DISPATCH_QUEUE_EMPTY, exception.errorCode)
    }

    @Test
    fun `배정에 실패하면(방어적 CAS) 예외가 발생한다`() {
        every { riderRepository.findByAccountId(100L) } returns availableRider()
        every { dispatchQueueRepository.claimNext() } returns DispatchQueueRow(deliveryId = 10L, orderId = 1L, shopId = 1L, estimatedPickupAt = null)
        every { deliveryAssignmentRepository.tryAssignRider(10L, 1L) } returns false

        val exception = assertThrows<BusinessException> { dispatchQueueService.claim(100L) }

        assertEquals(DeliveryErrorCode.DISPATCH_ALREADY_ASSIGNED, exception.errorCode)
    }

    @Test
    fun `정상적으로 클레임하면 배달을 배정받고 라이더는 BUSY가 된다`() {
        val rider = availableRider()
        every { riderRepository.findByAccountId(100L) } returns rider
        every { dispatchQueueRepository.claimNext() } returns DispatchQueueRow(deliveryId = 10L, orderId = 1L, shopId = 2L, estimatedPickupAt = null)
        every { deliveryAssignmentRepository.tryAssignRider(10L, 1L) } returns true
        every { dispatchOfferRepository.findAllByDeliveryId(10L) } returns emptyList()
        every { orderService.markRiderAssigned(1L) } returns Unit

        val actual = dispatchQueueService.claim(100L)

        assertEquals(10L, actual.deliveryId)
        assertEquals(RiderStatus.BUSY, rider.status)
        verify { orderService.markRiderAssigned(1L) }
    }

    @Test
    fun `클레임되면 같은 배달의 대기 중인 오퍼는 EXPIRED로 정리된다`() {
        every { riderRepository.findByAccountId(100L) } returns availableRider()
        every { dispatchQueueRepository.claimNext() } returns DispatchQueueRow(deliveryId = 10L, orderId = 1L, shopId = 2L, estimatedPickupAt = null)
        every { deliveryAssignmentRepository.tryAssignRider(10L, 1L) } returns true
        val pendingOffer = DispatchOffer.withId(5L, deliveryId = 10L, riderId = 2L)
        every { dispatchOfferRepository.findAllByDeliveryId(10L) } returns listOf(pendingOffer)
        every { orderService.markRiderAssigned(1L) } returns Unit

        dispatchQueueService.claim(100L)

        assertEquals(DispatchOfferStatus.EXPIRED, pendingOffer.status)
    }
}
