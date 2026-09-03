package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.Rider
import delivery.delivery.domain.RiderStatus
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.RiderRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.assertEquals

class DeliveryServiceTest {

    private val deliveryRepository = mockk<DeliveryRepository>()
    private val riderRepository = mockk<RiderRepository>()
    private lateinit var deliveryService: DeliveryService

    @BeforeEach
    fun setUp() {
        deliveryService = DeliveryService(deliveryRepository, riderRepository)
    }

    private fun assignedDelivery(riderId: Long = 1L): Delivery =
        Delivery.withId(10L, orderId = 1L, shopId = 1L, status = DeliveryStatus.ASSIGNED).apply { this.riderId = riderId }

    private fun pickedUpDelivery(riderId: Long = 1L): Delivery =
        Delivery.withId(10L, orderId = 1L, shopId = 1L, status = DeliveryStatus.PICKED_UP).apply { this.riderId = riderId }

    private fun rider(id: Long = 1L, accountId: Long = 100L, status: RiderStatus = RiderStatus.BUSY) = Rider.withId(id, accountId, status = status)

    @Test
    fun `존재하지 않는 배달을 픽업 처리하면 예외가 발생한다`() {
        every { deliveryRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { deliveryService.pickup(999L, 100L) }

        assertEquals(DeliveryErrorCode.DELIVERY_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `라이더 프로필이 없으면 픽업 처리에 실패한다`() {
        every { deliveryRepository.findById(10L) } returns Optional.of(assignedDelivery())
        every { riderRepository.findByAccountId(100L) } returns null

        val exception = assertThrows<BusinessException> { deliveryService.pickup(10L, 100L) }

        assertEquals(DeliveryErrorCode.RIDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `본인에게 배정되지 않은 배달을 픽업 처리하면 예외가 발생한다`() {
        every { deliveryRepository.findById(10L) } returns Optional.of(assignedDelivery(riderId = 2L))
        every { riderRepository.findByAccountId(100L) } returns rider(id = 1L, accountId = 100L)

        val exception = assertThrows<BusinessException> { deliveryService.pickup(10L, 100L) }

        assertEquals(DeliveryErrorCode.NOT_YOUR_DELIVERY, exception.errorCode)
    }

    @Test
    fun `ASSIGNED 상태의 배달을 픽업 처리하면 PICKED_UP으로 바뀐다`() {
        val delivery = assignedDelivery()
        every { deliveryRepository.findById(10L) } returns Optional.of(delivery)
        every { riderRepository.findByAccountId(100L) } returns rider()

        val actual = deliveryService.pickup(10L, 100L)

        assertEquals(DeliveryStatus.PICKED_UP, actual.status)
    }

    @Test
    fun `아직 배정되지 않은 배달을 픽업 처리하면 409가 발생한다`() {
        val delivery = Delivery.withId(10L, orderId = 1L, shopId = 1L, status = DeliveryStatus.OFFERING).apply { riderId = 1L }
        every { deliveryRepository.findById(10L) } returns Optional.of(delivery)
        every { riderRepository.findByAccountId(100L) } returns rider()

        val exception = assertThrows<BusinessException> { deliveryService.pickup(10L, 100L) }

        assertEquals(DeliveryErrorCode.INVALID_DELIVERY_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `본인에게 배정되지 않은 배달을 완료 처리하면 예외가 발생한다`() {
        every { deliveryRepository.findById(10L) } returns Optional.of(pickedUpDelivery(riderId = 2L))
        every { riderRepository.findByAccountId(100L) } returns rider(id = 1L, accountId = 100L)

        val exception = assertThrows<BusinessException> { deliveryService.complete(10L, 100L) }

        assertEquals(DeliveryErrorCode.NOT_YOUR_DELIVERY, exception.errorCode)
    }

    @Test
    fun `PICKED_UP 상태의 배달을 완료 처리하면 DELIVERED로 바뀌고 라이더는 다시 AVAILABLE이 된다`() {
        val delivery = pickedUpDelivery()
        val ridingRider = rider()
        every { deliveryRepository.findById(10L) } returns Optional.of(delivery)
        every { riderRepository.findByAccountId(100L) } returns ridingRider

        val actual = deliveryService.complete(10L, 100L)

        assertEquals(DeliveryStatus.DELIVERED, actual.status)
        assertEquals(RiderStatus.AVAILABLE, ridingRider.status)
    }

    @Test
    fun `픽업 전 배달을 완료 처리하면 409가 발생한다`() {
        val delivery = assignedDelivery()
        every { deliveryRepository.findById(10L) } returns Optional.of(delivery)
        every { riderRepository.findByAccountId(100L) } returns rider()

        val exception = assertThrows<BusinessException> { deliveryService.complete(10L, 100L) }

        assertEquals(DeliveryErrorCode.INVALID_DELIVERY_STATUS_TRANSITION, exception.errorCode)
    }
}
