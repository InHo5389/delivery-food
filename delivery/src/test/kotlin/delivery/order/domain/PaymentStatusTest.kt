package delivery.order.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaymentStatusTest {

    @Test
    fun `READY에서 AUTH로 전이할 수 있다`() {
        assertTrue(PaymentStatus.READY.canTransitionTo(PaymentStatus.AUTH))
    }

    @Test
    fun `READY에서 FAILED로 전이할 수 있다`() {
        assertTrue(PaymentStatus.READY.canTransitionTo(PaymentStatus.FAILED))
    }

    @Test
    fun `READY에서 APPROVED로 직접 전이할 수 없다`() {
        assertFalse(PaymentStatus.READY.canTransitionTo(PaymentStatus.APPROVED))
    }

    @Test
    fun `AUTH에서 APPROVED로 전이할 수 있다`() {
        assertTrue(PaymentStatus.AUTH.canTransitionTo(PaymentStatus.APPROVED))
    }

    @Test
    fun `AUTH에서 FAILED로 전이할 수 있다`() {
        assertTrue(PaymentStatus.AUTH.canTransitionTo(PaymentStatus.FAILED))
    }

    @Test
    fun `APPROVED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in PaymentStatus.entries) {
            assertFalse(PaymentStatus.APPROVED.canTransitionTo(target))
        }
    }

    @Test
    fun `FAILED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in PaymentStatus.entries) {
            assertFalse(PaymentStatus.FAILED.canTransitionTo(target))
        }
    }

    @Test
    fun `같은 상태로의 전이는 허용되지 않는다`() {
        for (status in PaymentStatus.entries) {
            assertFalse(status.canTransitionTo(status))
        }
    }
}
