package delivery.order.application.dto

import delivery.order.domain.Cart
import delivery.order.domain.CartItem

data class CartResult(
    val cart: Cart,
    val items: List<CartItem>,
) {
    val totalPrice: Long
        get() = items.sumOf { it.menuPrice * it.quantity }
}
