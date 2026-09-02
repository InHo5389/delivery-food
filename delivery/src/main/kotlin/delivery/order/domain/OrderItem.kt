package delivery.order.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// order_id는 같은 order 모듈 내 참조라 FK를 허용한다(1-8절 — 모듈 간 FK만 금지).
// menuName/menuPrice는 주문 생성 시점 스냅샷이다. 사장님이 이후 메뉴 가격을 바꿔도
// 이미 생성된 주문 항목은 영향받지 않는다("주문 당시엔 8,000원" 문제 방지).
@Entity
@Table(name = "order_items")
class OrderItem(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "menu_id", nullable = false)
    val menuId: Long,

    @Column(name = "menu_name", nullable = false)
    val menuName: String,

    @Column(name = "menu_price", nullable = false)
    val menuPrice: Long,

    @Column(name = "quantity", nullable = false)
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
            orderId: Long,
            menuId: Long,
            menuName: String,
            menuPrice: Long,
            quantity: Int,
        ): OrderItem = OrderItem(orderId, menuId, menuName, menuPrice, quantity).also { it.id = id }
    }
}
