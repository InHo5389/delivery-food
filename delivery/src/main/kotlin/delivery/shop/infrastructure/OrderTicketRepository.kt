package delivery.shop.infrastructure

import delivery.shop.domain.OrderTicket
import org.springframework.data.jpa.repository.JpaRepository

interface OrderTicketRepository : JpaRepository<OrderTicket, Long> {
    fun findByOrderId(orderId: Long): OrderTicket?
    fun findAllByShopIdOrderByCreatedAtDesc(shopId: Long): List<OrderTicket>
}
