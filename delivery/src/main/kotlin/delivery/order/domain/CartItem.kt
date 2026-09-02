package delivery.order.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// menuName/menuPrice는 담을 당시의 메뉴 정보를 복사해둔 것이다. Order의 스냅샷과 달리
// 이건 "불변 이력"이 아니라 화면에 바로 보여줄 표시용 캐시일 뿐이다 — 실제 주문 생성
// 시점에는 shopService를 통해 최신 가격을 다시 검증한다(커밋 37 참조).
@Entity
@Table(name = "cart_items")
class CartItem(
    @Column(name = "cart_id", nullable = false)
    val cartId: Long,

    @Column(name = "menu_id", nullable = false)
    val menuId: Long,

    @Column(name = "menu_name", nullable = false)
    val menuName: String,

    @Column(name = "menu_price", nullable = false)
    val menuPrice: Long,

    @Column(name = "quantity", nullable = false)
    var quantity: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun changeQuantity(quantity: Int) {
        this.quantity = quantity
        this.updatedAt = Instant.now()
    }

    companion object {
        fun withId(
            id: Long,
            cartId: Long,
            menuId: Long,
            menuName: String,
            menuPrice: Long,
            quantity: Int,
        ): CartItem = CartItem(cartId, menuId, menuName, menuPrice, quantity).also { it.id = id }
    }
}
