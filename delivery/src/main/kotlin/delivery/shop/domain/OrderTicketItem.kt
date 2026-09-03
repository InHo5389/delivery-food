package delivery.shop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// menuName/menuPrice는 주문 시점 스냅샷이다(OrderItem과 동일한 이유) — 사장님이
// 나중에 메뉴 가격을 바꿔도 이미 접수된 티켓의 항목 표시는 바뀌지 않는다.
@Entity
@Table(name = "order_ticket_item")
class OrderTicketItem(
    @Column(name = "order_ticket_id", nullable = false)
    val orderTicketId: Long,

    @Column(name = "menu_name", nullable = false)
    val menuName: String,

    @Column(name = "menu_price", nullable = false)
    val menuPrice: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun withId(
            id: Long,
            orderTicketId: Long,
            menuName: String,
            menuPrice: Long,
            quantity: Int,
        ): OrderTicketItem = OrderTicketItem(orderTicketId, menuName, menuPrice, quantity).also { it.id = id }
    }
}
