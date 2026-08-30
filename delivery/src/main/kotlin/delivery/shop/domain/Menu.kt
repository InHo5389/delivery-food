package delivery.shop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "menu")
class Menu(
    @Column(name = "shop_id", nullable = false)
    val shopId: Long,

    @Column(name = "menu_group_id", nullable = false)
    val menuGroupId: Long,

    @Column(nullable = false)
    var name: String,

    @Column
    var description: String? = null,

    @Column(nullable = false)
    var price: Long,

    @Column(name = "sold_out", nullable = false)
    var soldOut: Boolean = false,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun withId(
            id: Long,
            shopId: Long,
            menuGroupId: Long,
            name: String,
            price: Long,
            displayOrder: Int,
        ): Menu = Menu(shopId, menuGroupId, name, price = price, displayOrder = displayOrder).also { it.id = id }
    }
}
