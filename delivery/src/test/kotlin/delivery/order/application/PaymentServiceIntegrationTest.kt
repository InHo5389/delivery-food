package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.order.application.dto.RequestPaymentCommand
import delivery.order.domain.Order
import delivery.order.domain.PaymentErrorCode
import delivery.order.domain.PaymentStatus
import delivery.order.infrastructure.OrderRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals

class PaymentServiceIntegrationTest(
    @Autowired private val paymentService: PaymentService,
    @Autowired private val orderRepository: OrderRepository,
) : IntegrationTestSupport() {

    private fun newOrderId(): Long =
        orderRepository.save(Order(customerId = System.nanoTime(), shopId = 1L, customerName = "홍길동", customerPhone = "01011112222")).id!!

    @Test
    fun `실패율 0인 환경에서 결제를 요청하면 승인된다`() {
        val actual = paymentService.requestPayment(RequestPaymentCommand(orderId = newOrderId(), amount = 8000L))

        assertEquals(PaymentStatus.APPROVED, actual.status)
    }

    @Test
    fun `결제 완료 후 주문 ID로 조회할 수 있다`() {
        val orderId = newOrderId()
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

    @Test
    fun `승인된 결제를 환불하면 REFUNDED 상태가 된다`() {
        val orderId = newOrderId()
        paymentService.requestPayment(RequestPaymentCommand(orderId = orderId, amount = 8000L))

        val actual = paymentService.refund(orderId)

        assertEquals(PaymentStatus.REFUNDED, actual.status)
    }

    @Test
    fun `이미 환불된 결제를 다시 환불하면 예외가 발생한다`() {
        val orderId = newOrderId()
        paymentService.requestPayment(RequestPaymentCommand(orderId = orderId, amount = 8000L))
        paymentService.refund(orderId)

        val exception = assertThrows<BusinessException> { paymentService.refund(orderId) }

        assertEquals(delivery.order.domain.PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION, exception.errorCode)
    }
}

@TestPropertySource(properties = ["payment.mock-pg.failure-rate=1.0"])
class PaymentServiceAlwaysFailIntegrationTest(
    @Autowired private val paymentService: PaymentService,
    @Autowired private val orderRepository: OrderRepository,
) : IntegrationTestSupport() {

    @Test
    fun `실패율 1인 환경에서 결제를 요청하면 거절된다`() {
        val orderId = orderRepository.save(
            Order(customerId = System.nanoTime(), shopId = 1L, customerName = "홍길동", customerPhone = "01011112222")
        ).id!!

        val actual = paymentService.requestPayment(RequestPaymentCommand(orderId = orderId, amount = 8000L))

        assertEquals(PaymentStatus.FAILED, actual.status)
    }

    @Test
    fun `결제가 FAILED 상태일 때 환불을 요청하면 상태 전이 예외가 발생한다`() {
        // 이 클래스는 failure-rate=1.0이라 결제가 항상 FAILED가 된다.
        // FAILED는 REFUNDED로 전이할 수 없으므로 PG 환불 호출 전에 상태 전이 예외가 발생해야 한다
        // (PaymentService.refund가 상태 전이 가능 여부를 PG 호출보다 먼저 확인하는 것을 검증).
        val orderId = orderRepository.save(
            Order(customerId = System.nanoTime(), shopId = 1L, customerName = "홍길동", customerPhone = "01011112222")
        ).id!!
        paymentService.requestPayment(RequestPaymentCommand(orderId = orderId, amount = 8000L))

        val exception = assertThrows<BusinessException> { paymentService.refund(orderId) }

        assertEquals(delivery.order.domain.PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION, exception.errorCode)
    }
}
