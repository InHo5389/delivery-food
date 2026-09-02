package delivery.order.application.dto

data class AddCartItemCommand(
    val customerId: Long,
    val shopId: Long,
    val menuId: Long,
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
)

data class ChangeCartItemQuantityCommand(
    val customerId: Long,
    val cartItemId: Long,
    val quantity: Int,
)
