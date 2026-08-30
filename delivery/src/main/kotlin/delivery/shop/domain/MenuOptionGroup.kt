package delivery.shop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "menu_option_group")
class MenuOptionGroup(
    @Column(name = "menu_id", nullable = false)
    val menuId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var required: Boolean = false,

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
        fun withId(id: Long, menuId: Long, name: String, required: Boolean, displayOrder: Int): MenuOptionGroup =
            MenuOptionGroup(menuId, name, required, displayOrder).also { it.id = id }
    }
}
