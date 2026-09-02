package delivery.order.api.dto

import jakarta.validation.constraints.Min

data class AddCartItemRequest(
    val shopId: Long,
    val menuId: Long,
    val menuName: String,
    val menuPrice: Long,
    @field:Min(1)
    val quantity: Int,
)

data class ChangeCartItemQuantityRequest(
    @field:Min(1)
    val quantity: Int,
)
