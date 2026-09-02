package delivery.order.infrastructure

import delivery.order.domain.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByCustomerId(customerId: Long): List<Order>
}
