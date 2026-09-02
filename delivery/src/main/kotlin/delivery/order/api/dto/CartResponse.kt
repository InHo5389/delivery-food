package delivery.order.api.dto

import delivery.order.application.dto.CartResult

data class CartResponse(
    val cartId: Long,
    val shopId: Long,
    val items: List<CartItemResponse>,
    val totalPrice: Long,
) {
    companion object {
        fun from(result: CartResult): CartResponse =
            CartResponse(
                cartId = result.cart.id!!,
                shopId = result.cart.shopId,
                items = result.items.map {
                    CartItemResponse(it.id!!, it.menuId, it.menuName, it.menuPrice, it.quantity)
                },
                totalPrice = result.totalPrice,
            )
    }
}

data class CartItemResponse(
    val cartItemId: Long,
    val menuId: Long,
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
)
