package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.order.application.dto.RequestPaymentCommand
import delivery.order.domain.PaymentErrorCode
import delivery.order.domain.PaymentStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals

class PaymentServiceIntegrationTest(
    @Autowired private val paymentService: PaymentService,
) : IntegrationTestSupport() {

    @Test
    fun `실패율 0인 환경에서 결제를 요청하면 승인된다`() {
        val actual = paymentService.requestPayment(RequestPaymentCommand(orderId = System.nanoTime(), amount = 8000L))

        assertEquals(PaymentStatus.APPROVED, actual.status)
    }

    @Test
    fun `결제 완료 후 주문 ID로 조회할 수 있다`() {
        val orderId = System.nanoTime()
        paymentService.requestPayment(RequestPaymentCommand(orderId = orderId, amount = 8000L))

        val actual = paymentService.getByOrderId(orderId)

        assertEquals(orderId, actual.orderId)
        assertEquals(PaymentStatus.APPROVED, actual.status)
    }

    @Test
    fun `존재하지 않는 주문의 결제를 조회하면 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { paymentService.getByOrderId(Long.MAX_VALUE) }

        assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, exception.errorCode)
    }
}

@TestPropertySource(properties = ["payment.mock-pg.failure-rate=1.0"])
class PaymentServiceAlwaysFailIntegrationTest(
    @Autowired private val paymentService: PaymentService,
) : IntegrationTestSupport() {

    @Test
    fun `실패율 1인 환경에서 결제를 요청하면 거절된다`() {
        val actual = paymentService.requestPayment(RequestPaymentCommand(orderId = System.nanoTime(), amount = 8000L))

        assertEquals(PaymentStatus.FAILED, actual.status)
    }
}
