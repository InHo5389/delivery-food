package delivery.order.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// shop_id는 다른 모듈(shop) 테이블에 대한 논리 참조라 FK를 걸지 않는다(1-8절).
// 고객 1명은 한 번에 한 상점의 메뉴만 담을 수 있다 — 배달앱 실무 관례와 동일하게,
// 다른 상점 메뉴를 담으려 하면 기존 장바구니를 비우도록 안내한다(addItem에서 검증).
@Entity
@Table(name = "cart")
class Cart(
    @Column(name = "customer_id", nullable = false)
    val customerId: Long,

    @Column(name = "shop_id", nullable = false)
    var shopId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun replaceShop(shopId: Long) {
        this.shopId = shopId
        this.updatedAt = Instant.now()
    }

    fun touch() {
        this.updatedAt = Instant.now()
    }

    companion object {
        fun withId(id: Long, customerId: Long, shopId: Long): Cart =
            Cart(customerId, shopId).also { it.id = id }
    }
}
