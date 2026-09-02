package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.order.application.dto.RequestPaymentCommand
import delivery.order.domain.Payment
import delivery.order.domain.PaymentErrorCode
import delivery.order.domain.PaymentStatus
import delivery.order.infrastructure.MockPgClient
import delivery.order.infrastructure.PaymentRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PaymentServiceTest {

    private val paymentRepository = mockk<PaymentRepository>()
    private val mockPgClient = mockk<MockPgClient>()
    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun setUp() {
        paymentService = PaymentService(paymentRepository, mockPgClient)
    }

    @Test
    fun `PG가 승인하면 결제 상태가 APPROVED가 된다`() {
        val command = RequestPaymentCommand(orderId = 1L, amount = 8000L)
        every { paymentRepository.save(any()) } answers { it.invocation.args[0] as Payment }
        every { mockPgClient.authorize(1L, 8000L) } returns true

        val actual = paymentService.requestPayment(command)

        assertEquals(PaymentStatus.APPROVED, actual.status)
    }

    @Test
    fun `PG가 거절하면 결제 상태가 FAILED가 된다`() {
        val command = RequestPaymentCommand(orderId = 1L, amount = 8000L)
        every { paymentRepository.save(any()) } answers { it.invocation.args[0] as Payment }
        every { mockPgClient.authorize(1L, 8000L) } returns false

        val actual = paymentService.requestPayment(command)

        assertEquals(PaymentStatus.FAILED, actual.status)
    }

    @Test
    fun `주문 ID로 결제 정보를 조회한다`() {
        val payment = Payment.withId(1L, 1L, 8000L, PaymentStatus.APPROVED)
        every { paymentRepository.findByOrderId(1L) } returns payment

        val actual = paymentService.getByOrderId(1L)

        assertEquals(PaymentStatus.APPROVED, actual.status)
    }

    @Test
    fun `존재하지 않는 주문의 결제를 조회하면 예외가 발생한다`() {
        every { paymentRepository.findByOrderId(999L) } returns null

        val exception = assertThrows<BusinessException> { paymentService.getByOrderId(999L) }

        assertEquals(PaymentErrorCode.PAYMENT_NOT_FOUND, exception.errorCode)
    }
}
