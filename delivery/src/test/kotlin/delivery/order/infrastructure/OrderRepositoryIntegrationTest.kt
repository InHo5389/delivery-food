package delivery.order.infrastructure

import delivery.order.domain.Order
import delivery.order.domain.OrderStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertTrue

class OrderRepositoryIntegrationTest(
    @Autowired private val orderRepository: OrderRepository,
) : IntegrationTestSupport() {

    private fun paidOrder(updatedAt: Instant): Order {
        val order = orderRepository.save(Order(System.nanoTime(), 1L, "홍길동", "01011112222"))
        order.transitionTo(OrderStatus.PAID)
        order.updatedAt = updatedAt
        return orderRepository.save(order)
    }

    @Test
    fun `threshold 이전에 PAID로 바뀐 주문만 조회된다`() {
        val stale = paidOrder(Instant.now().minus(10, ChronoUnit.MINUTES))
        val fresh = paidOrder(Instant.now())
        val threshold = Instant.now().minus(3, ChronoUnit.MINUTES)

        val actual = orderRepository.findAllByStatusAndUpdatedAtBefore(OrderStatus.PAID, threshold)

        assertTrue(actual.any { it.id == stale.id })
        assertTrue(actual.none { it.id == fresh.id })
    }

    @Test
    fun `CREATED 상태의 주문은 threshold와 무관하게 조회되지 않는다`() {
        val order = orderRepository.save(Order(System.nanoTime(), 1L, "홍길동", "01011112222"))
        order.updatedAt = Instant.now().minus(10, ChronoUnit.MINUTES)
        orderRepository.save(order)
        val threshold = Instant.now().minus(3, ChronoUnit.MINUTES)

        val actual = orderRepository.findAllByStatusAndUpdatedAtBefore(OrderStatus.PAID, threshold)

        assertTrue(actual.none { it.id == order.id })
    }
}
