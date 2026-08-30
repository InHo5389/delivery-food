package delivery.shop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "menu_group")
class MenuGroup(
    @Column(name = "shop_id", nullable = false)
    val shopId: Long,

    @Column(nullable = false)
    var name: String,

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
        fun withId(id: Long, shopId: Long, name: String, displayOrder: Int): MenuGroup =
            MenuGroup(shopId, name, displayOrder).also { it.id = id }
    }
}
