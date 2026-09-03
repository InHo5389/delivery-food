package delivery.shop.infrastructure

import delivery.shop.domain.OrderTicketItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderTicketItemRepository : JpaRepository<OrderTicketItem, Long> {
    fun findAllByOrderTicketId(orderTicketId: Long): List<OrderTicketItem>
}
