package delivery.delivery.domain

import delivery.common.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeliveryTest {

    private fun newDelivery(): Delivery = Delivery(
        orderId = 1L,
        shopId = 1L,
        pickupLatitude = BigDecimal("37.5665000"),
        pickupLongitude = BigDecimal("126.9780000"),
    )

    @Test
    fun `신규 생성 시 id는 null이다`() {
        val delivery = newDelivery()

        assertNull(delivery.id)
    }

    @Test
    fun `신규 생성 시 기본 상태는 PENDING이다`() {
        val delivery = newDelivery()

        assertEquals(DeliveryStatus.PENDING, delivery.status)
    }

    @Test
    fun `신규 생성 시 배정된 라이더가 없다`() {
        val delivery = newDelivery()

        assertNull(delivery.riderId)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val delivery = Delivery.withId(id = 10L, orderId = 1L, shopId = 1L)

        assertEquals(10L, delivery.id)
    }

    @Test
    fun `허용된 전이면 상태가 바뀐다`() {
        val delivery = newDelivery()

        delivery.transitionTo(DeliveryStatus.OFFERING)

        assertEquals(DeliveryStatus.OFFERING, delivery.status)
    }

    @Test
    fun `허용되지 않은 전이는 예외가 발생하고 상태가 바뀌지 않는다`() {
        val delivery = newDelivery()

        val exception = assertThrows<BusinessException> { delivery.transitionTo(DeliveryStatus.DELIVERED) }

        assertEquals(DeliveryErrorCode.INVALID_DELIVERY_STATUS_TRANSITION, exception.errorCode)
        assertEquals(DeliveryStatus.PENDING, delivery.status)
    }
}
