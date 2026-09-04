package delivery.order.infrastructure

import delivery.order.domain.Order
import delivery.order.domain.OrderItem
import delivery.order.domain.OrderStatus
import delivery.order.domain.Payment
import delivery.order.domain.PaymentStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopSettlementSourceRepositoryIntegrationTest(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val orderItemRepository: OrderItemRepository,
    @Autowired private val paymentRepository: PaymentRepository,
    @Autowired private val shopSettlementSourceRepository: ShopSettlementSourceRepository,
) : IntegrationTestSupport() {

    private fun deliveredOrder(shopId: Long, deliveredAt: Instant, menuPrice: Long, quantity: Int): Order {
        val order = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        order.transitionTo(OrderStatus.PAID)
        order.transitionTo(OrderStatus.ACCEPTED)
        order.transitionTo(OrderStatus.RIDER_ASSIGNED)
        order.transitionTo(OrderStatus.PICKED_UP)
        order.transitionTo(OrderStatus.DELIVERED)
        order.updatedAt = deliveredAt
        orderRepository.save(order)
        orderItemRepository.save(OrderItem(orderId = order.id!!, menuId = 1L, menuName = "짜장면", menuPrice = menuPrice, quantity = quantity))
        return order
    }

    private fun refundedPayment(shopId: Long, refundedAt: Instant, amount: Long): Order {
        val order = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        val payment = paymentRepository.save(Payment(orderId = order.id!!, amount = amount))
        payment.transitionTo(PaymentStatus.AUTH)
        payment.transitionTo(PaymentStatus.APPROVED)
        payment.transitionTo(PaymentStatus.REFUNDED)
        payment.updatedAt = refundedAt
        paymentRepository.save(payment)
        return order
    }

    @Test
    fun `범위 안의 DELIVERED 주문 금액을 주문 단위로 반환한다`() {
        val shopId = System.nanoTime()
        val now = Instant.now()
        val order = deliveredOrder(shopId, now, menuPrice = 8000L, quantity = 2)
        // 범위 밖(어제)
        deliveredOrder(shopId, now.minus(1, ChronoUnit.DAYS), menuPrice = 100_000L, quantity = 1)

        val actual = shopSettlementSourceRepository.findDeliveredOrderAmounts(shopId, now.minusSeconds(3600), now.plusSeconds(3600))

        assertEquals(1, actual.size)
        assertEquals(order.id, actual[0].orderId)
        assertEquals(16_000L, actual[0].amount)
    }

    @Test
    fun `DELIVERED가 아닌 주문은 제외된다`() {
        val shopId = System.nanoTime()
        val now = Instant.now()
        val notYetDelivered = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        notYetDelivered.transitionTo(OrderStatus.PAID)
        notYetDelivered.updatedAt = now
        orderRepository.save(notYetDelivered)
        orderItemRepository.save(OrderItem(orderId = notYetDelivered.id!!, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1))

        val actual = shopSettlementSourceRepository.findDeliveredOrderAmounts(shopId, now.minusSeconds(60), now.plusSeconds(60))

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `주문이 없는 상점을 조회하면 빈 목록을 반환한다`() {
        val actual = shopSettlementSourceRepository.findDeliveredOrderAmounts(Long.MAX_VALUE, Instant.EPOCH, Instant.now())

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `범위 안의 REFUNDED 결제 금액을 주문 단위로 반환한다`() {
        val shopId = System.nanoTime()
        val now = Instant.now()
        val order = refundedPayment(shopId, now, amount = 10_000L)
        // 범위 밖(어제)
        refundedPayment(shopId, now.minus(1, ChronoUnit.DAYS), amount = 5_000L)

        val actual = shopSettlementSourceRepository.findRefundedPaymentAmounts(shopId, now.minusSeconds(3600), now.plusSeconds(3600))

        assertEquals(1, actual.size)
        assertEquals(order.id, actual[0].orderId)
        assertEquals(10_000L, actual[0].amount)
    }

    @Test
    fun `REFUNDED가 아닌 결제는 제외된다`() {
        val shopId = System.nanoTime()
        val now = Instant.now()
        val order = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        val payment = paymentRepository.save(Payment(orderId = order.id!!, amount = 10_000L))
        payment.transitionTo(PaymentStatus.AUTH)
        payment.transitionTo(PaymentStatus.APPROVED)
        payment.updatedAt = now
        paymentRepository.save(payment)

        val actual = shopSettlementSourceRepository.findRefundedPaymentAmounts(shopId, now.minusSeconds(60), now.plusSeconds(60))

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `환불된 결제가 없는 상점을 조회하면 빈 목록을 반환한다`() {
        val actual = shopSettlementSourceRepository.findRefundedPaymentAmounts(Long.MAX_VALUE, Instant.EPOCH, Instant.now())

        assertTrue(actual.isEmpty())
    }
}
