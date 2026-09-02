package delivery.order.infrastructure

import delivery.order.domain.CartItem
import org.springframework.data.jpa.repository.JpaRepository

interface CartItemRepository : JpaRepository<CartItem, Long> {
    fun findAllByCartId(cartId: Long): List<CartItem>
    fun findByCartIdAndMenuId(cartId: Long, menuId: Long): CartItem?
    fun deleteAllByCartId(cartId: Long)
}
