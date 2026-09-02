package delivery.order.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderStatusTest {

    @Test
    fun `CREATED에서 PAID로 전이할 수 있다`() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.PAID))
    }

    @Test
    fun `CREATED에서 PAYMENT_FAILED로 전이할 수 있다`() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.PAYMENT_FAILED))
    }

    @Test
    fun `CREATED에서 CANCELLED로 전이할 수 있다`() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED))
    }

    @Test
    fun `CREATED에서 ACCEPTED로 직접 전이할 수 없다`() {
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.ACCEPTED))
    }

    @Test
    fun `PAID에서 ACCEPTED로 전이할 수 있다`() {
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.ACCEPTED))
    }

    @Test
    fun `PAID에서 REJECTED로 전이할 수 있다`() {
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.REJECTED))
    }

    @Test
    fun `PAID에서 CANCELLED로 전이할 수 있다 (수락 전 자유 취소)`() {
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED))
    }

    @Test
    fun `ACCEPTED에서 CANCELLED로 전이할 수 없다 (수락 후 자유 취소 불가)`() {
        assertFalse(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.CANCELLED))
    }

    @Test
    fun `ACCEPTED에서 COOKING으로 전이할 수 있다`() {
        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.COOKING))
    }

    @Test
    fun `COOKING에서 COOKED로 전이할 수 있다`() {
        assertTrue(OrderStatus.COOKING.canTransitionTo(OrderStatus.COOKED))
    }

    @Test
    fun `COOKING에서 DELIVERED로 직접 전이할 수 없다`() {
        assertFalse(OrderStatus.COOKING.canTransitionTo(OrderStatus.DELIVERED))
    }

    @Test
    fun `COOKED에서 RIDER_ASSIGNED로 전이할 수 있다`() {
        assertTrue(OrderStatus.COOKED.canTransitionTo(OrderStatus.RIDER_ASSIGNED))
    }

    @Test
    fun `RIDER_ASSIGNED에서 PICKED_UP으로 전이할 수 있다`() {
        assertTrue(OrderStatus.RIDER_ASSIGNED.canTransitionTo(OrderStatus.PICKED_UP))
    }

    @Test
    fun `PICKED_UP에서 DELIVERED로 전이할 수 있다`() {
        assertTrue(OrderStatus.PICKED_UP.canTransitionTo(OrderStatus.DELIVERED))
    }

    @Test
    fun `DELIVERED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in OrderStatus.entries) {
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(target))
        }
    }

    @Test
    fun `CANCELLED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in OrderStatus.entries) {
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target))
        }
    }

    @Test
    fun `REJECTED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in OrderStatus.entries) {
            assertFalse(OrderStatus.REJECTED.canTransitionTo(target))
        }
    }

    @Test
    fun `PAYMENT_FAILED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in OrderStatus.entries) {
            assertFalse(OrderStatus.PAYMENT_FAILED.canTransitionTo(target))
        }
    }

    @Test
    fun `같은 상태로의 전이는 허용되지 않는다`() {
        for (status in OrderStatus.entries) {
            assertFalse(status.canTransitionTo(status))
        }
    }
}
