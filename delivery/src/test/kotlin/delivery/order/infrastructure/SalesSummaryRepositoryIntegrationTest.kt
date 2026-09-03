package delivery.order.infrastructure

import delivery.order.domain.Order
import delivery.order.domain.OrderItem
import delivery.order.domain.OrderStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class SalesSummaryRepositoryIntegrationTest(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val orderItemRepository: OrderItemRepository,
    @Autowired private val salesSummaryRepository: SalesSummaryRepository,
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

    @Test
    fun `범위 안의 DELIVERED 주문만 합산한다`() {
        val shopId = System.nanoTime()
        val now = Instant.now()
        deliveredOrder(shopId, now, menuPrice = 8000L, quantity = 2)
        deliveredOrder(shopId, now, menuPrice = 9000L, quantity = 1)
        // 범위 밖(어제)
        deliveredOrder(shopId, now.minus(1, ChronoUnit.DAYS), menuPrice = 100_000L, quantity = 1)

        val from = now.minusSeconds(3600)
        val to = now.plusSeconds(3600)
        val actual = salesSummaryRepository.findSales(shopId, from, to)

        assertEquals(2L, actual.orderCount)
        assertEquals(25000L, actual.totalAmount)
    }

    @Test
    fun `DELIVERED가 아닌 주문은 집계에서 제외된다`() {
        val shopId = System.nanoTime()
        val now = Instant.now()
        val notYetDelivered = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        notYetDelivered.transitionTo(OrderStatus.PAID)
        notYetDelivered.updatedAt = now
        orderRepository.save(notYetDelivered)
        orderItemRepository.save(OrderItem(orderId = notYetDelivered.id!!, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1))

        val actual = salesSummaryRepository.findSales(shopId, now.minusSeconds(60), now.plusSeconds(60))

        assertEquals(0L, actual.orderCount)
        assertEquals(0L, actual.totalAmount)
    }

    @Test
    fun `다른 상점의 매출은 섞이지 않는다`() {
        val shopId = System.nanoTime()
        val otherShopId = System.nanoTime() + 1
        val now = Instant.now()
        deliveredOrder(shopId, now, menuPrice = 8000L, quantity = 1)
        deliveredOrder(otherShopId, now, menuPrice = 999_000L, quantity = 1)

        val actual = salesSummaryRepository.findSales(shopId, now.minusSeconds(60), now.plusSeconds(60))

        assertEquals(1L, actual.orderCount)
        assertEquals(8000L, actual.totalAmount)
    }

    @Test
    fun `해당하는 주문이 없으면 0을 반환한다`() {
        val shopId = System.nanoTime()
        val now = Instant.now()

        val actual = salesSummaryRepository.findSales(shopId, now.minusSeconds(60), now.plusSeconds(60))

        assertEquals(0L, actual.orderCount)
        assertEquals(0L, actual.totalAmount)
    }

    @Test
    fun `주문 항목이 여러 개면 항목별 금액을 모두 합산한다`() {
        val shopId = System.nanoTime()
        val now = Instant.now()
        val order = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        order.transitionTo(OrderStatus.PAID)
        order.transitionTo(OrderStatus.ACCEPTED)
        order.transitionTo(OrderStatus.RIDER_ASSIGNED)
        order.transitionTo(OrderStatus.PICKED_UP)
        order.transitionTo(OrderStatus.DELIVERED)
        order.updatedAt = now
        orderRepository.save(order)
        orderItemRepository.save(OrderItem(orderId = order.id!!, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 2))
        orderItemRepository.save(OrderItem(orderId = order.id!!, menuId = 2L, menuName = "탕수육", menuPrice = 15000L, quantity = 1))

        val actual = salesSummaryRepository.findSales(shopId, now.minusSeconds(60), now.plusSeconds(60))

        assertEquals(1L, actual.orderCount)
        assertEquals(31000L, actual.totalAmount)
    }
}
