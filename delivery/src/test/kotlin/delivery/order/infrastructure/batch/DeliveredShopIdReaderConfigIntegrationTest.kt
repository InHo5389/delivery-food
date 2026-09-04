package delivery.order.infrastructure.batch

import delivery.order.domain.Order
import delivery.order.domain.OrderItem
import delivery.order.domain.OrderStatus
import delivery.order.infrastructure.OrderItemRepository
import delivery.order.infrastructure.OrderRepository
import delivery.support.IntegrationTestSupport
import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.batch.infrastructure.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeliveredShopIdReaderConfigIntegrationTest(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val orderItemRepository: OrderItemRepository,
    @Autowired private val entityManagerFactory: EntityManagerFactory,
) : IntegrationTestSupport() {

    private val zone = ZoneId.of("Asia/Seoul")
    private val config = DeliveredShopIdReaderConfig()

    private fun deliveredOrder(shopId: Long, deliveredAt: Instant): Order {
        val order = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        order.transitionTo(OrderStatus.PAID)
        order.transitionTo(OrderStatus.ACCEPTED)
        order.transitionTo(OrderStatus.RIDER_ASSIGNED)
        order.transitionTo(OrderStatus.PICKED_UP)
        order.transitionTo(OrderStatus.DELIVERED)
        order.updatedAt = deliveredAt
        orderRepository.save(order)
        orderItemRepository.save(OrderItem(orderId = order.id!!, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1))
        return order
    }

    private fun readAll(weekStart: LocalDate): List<Long> {
        val reader = config.deliveredShopIdReader(entityManagerFactory, weekStart.toString()) as ItemStreamReader<Long>
        val items = mutableListOf<Long>()
        reader.open(ExecutionContext())
        try {
            while (true) {
                val item = reader.read() ?: break
                items.add(item)
            }
        } finally {
            reader.close()
        }
        return items
    }

    @Test
    fun `그 주에 배달완료된 주문의 상점 id를 중복 없이 반환한다`() {
        val shopId = System.nanoTime()
        val monday = LocalDate.of(2026, 3, 23)
        val midweek = monday.plusDays(2).atTime(12, 0).atZone(zone).toInstant()
        // 같은 상점이 그 주에 두 건 완료해도 한 번만 나와야 한다.
        deliveredOrder(shopId, midweek)
        deliveredOrder(shopId, midweek.plusSeconds(3600))
        // 범위 밖(전주)
        deliveredOrder(shopId, midweek.minusSeconds(7 * 86400))

        val actual = readAll(monday)

        assertEquals(listOf(shopId), actual)
    }

    @Test
    fun `그 주에 배달완료된 주문이 없으면 빈 목록을 반환한다`() {
        val monday = LocalDate.of(2026, 3, 30)

        val actual = readAll(monday)

        assertTrue(actual.isEmpty())
    }
}
