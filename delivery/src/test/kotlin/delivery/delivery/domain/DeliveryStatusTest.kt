package delivery.delivery.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeliveryStatusTest {

    @Test
    fun `PENDING에서 OFFERING으로 전이할 수 있다`() {
        assertTrue(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.OFFERING))
    }

    @Test
    fun `PENDING에서 CANCELLED로 전이할 수 있다`() {
        assertTrue(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.CANCELLED))
    }

    @Test
    fun `PENDING에서 ASSIGNED로 직접 전이할 수 없다`() {
        assertFalse(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.ASSIGNED))
    }

    @Test
    fun `OFFERING에서 ASSIGNED로 전이할 수 있다`() {
        assertTrue(DeliveryStatus.OFFERING.canTransitionTo(DeliveryStatus.ASSIGNED))
    }

    @Test
    fun `OFFERING에서 FAILED로 전이할 수 있다`() {
        assertTrue(DeliveryStatus.OFFERING.canTransitionTo(DeliveryStatus.FAILED))
    }

    @Test
    fun `OFFERING에서 CANCELLED로 전이할 수 있다`() {
        assertTrue(DeliveryStatus.OFFERING.canTransitionTo(DeliveryStatus.CANCELLED))
    }

    @Test
    fun `OFFERING에서 PICKED_UP으로 직접 전이할 수 없다`() {
        assertFalse(DeliveryStatus.OFFERING.canTransitionTo(DeliveryStatus.PICKED_UP))
    }

    @Test
    fun `ASSIGNED에서 PICKED_UP으로 전이할 수 있다`() {
        assertTrue(DeliveryStatus.ASSIGNED.canTransitionTo(DeliveryStatus.PICKED_UP))
    }

    @Test
    fun `ASSIGNED에서 CANCELLED로 전이할 수 있다`() {
        assertTrue(DeliveryStatus.ASSIGNED.canTransitionTo(DeliveryStatus.CANCELLED))
    }

    @Test
    fun `PICKED_UP에서 DELIVERED로 전이할 수 있다`() {
        assertTrue(DeliveryStatus.PICKED_UP.canTransitionTo(DeliveryStatus.DELIVERED))
    }

    @Test
    fun `PICKED_UP에서 CANCELLED로 전이할 수 없다`() {
        assertFalse(DeliveryStatus.PICKED_UP.canTransitionTo(DeliveryStatus.CANCELLED))
    }

    @Test
    fun `DELIVERED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in DeliveryStatus.entries) {
            assertFalse(DeliveryStatus.DELIVERED.canTransitionTo(target))
        }
    }

    @Test
    fun `CANCELLED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in DeliveryStatus.entries) {
            assertFalse(DeliveryStatus.CANCELLED.canTransitionTo(target))
        }
    }

    @Test
    fun `FAILED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in DeliveryStatus.entries) {
            assertFalse(DeliveryStatus.FAILED.canTransitionTo(target))
        }
    }

    @Test
    fun `같은 상태로의 전이는 허용되지 않는다`() {
        for (status in DeliveryStatus.entries) {
            assertFalse(status.canTransitionTo(status))
        }
    }
}
