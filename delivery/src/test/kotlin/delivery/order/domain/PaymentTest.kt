package delivery.order.domain

import delivery.common.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentTest {

    @Test
    fun `신규 생성 시 id는 null이다`() {
        val payment = Payment(orderId = 1L, amount = 8000L)

        assertNull(payment.id)
    }

    @Test
    fun `신규 생성 시 기본 상태는 READY이다`() {
        val payment = Payment(orderId = 1L, amount = 8000L)

        assertEquals(PaymentStatus.READY, payment.status)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val payment = Payment.withId(id = 10L, orderId = 1L, amount = 8000L)

        assertEquals(10L, payment.id)
    }

    @Test
    fun `허용된 전이면 상태가 바뀐다`() {
        val payment = Payment(orderId = 1L, amount = 8000L)

        payment.transitionTo(PaymentStatus.AUTH)

        assertEquals(PaymentStatus.AUTH, payment.status)
    }

    @Test
    fun `허용되지 않은 전이는 예외가 발생하고 상태가 바뀌지 않는다`() {
        val payment = Payment(orderId = 1L, amount = 8000L)

        val exception = assertThrows<BusinessException> { payment.transitionTo(PaymentStatus.APPROVED) }

        assertEquals(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION, exception.errorCode)
        assertEquals(PaymentStatus.READY, payment.status)
    }
}
