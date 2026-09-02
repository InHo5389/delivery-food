package delivery.order.infrastructure

import delivery.order.domain.OrderItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findAllByOrderId(orderId: Long): List<OrderItem>
    fun findAllByOrderIdIn(orderIds: List<Long>): List<OrderItem>
}
