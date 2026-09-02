package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.order.application.dto.RequestPaymentCommand
import delivery.order.domain.Payment
import delivery.order.domain.PaymentErrorCode
import delivery.order.domain.PaymentStatus
import delivery.order.infrastructure.MockPgClient
import delivery.order.infrastructure.PaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 결제 성공/실패는 Payment의 상태로만 표현한다. 이 상태를 받아 Order를 CREATED에서
// PAID 또는 PAYMENT_FAILED로 전이시키는 것은 호출자(커밋 37 주문 생성 API)의 책임이다 —
// "주문을 먼저 만들고 결제 결과에 따라 전이한다"는 결정(가이드 문서 커밋 36)에 따라
// 두 애그리거트(Order, Payment)의 책임을 분리해둔다.
@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val mockPgClient: MockPgClient,
) {
    @Transactional
    fun requestPayment(command: RequestPaymentCommand): Payment {
        val payment = paymentRepository.save(Payment(orderId = command.orderId, amount = command.amount))
        payment.transitionTo(PaymentStatus.AUTH)

        val approved = mockPgClient.authorize(command.orderId, command.amount)
        payment.transitionTo(if (approved) PaymentStatus.APPROVED else PaymentStatus.FAILED)

        return payment
    }

    fun getByOrderId(orderId: Long): Payment =
        paymentRepository.findByOrderId(orderId) ?: throw BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND)
}
