package delivery.shop.application.dto

data class OrderTicketItemCommand(
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
)

data class CreateOrderTicketCommand(
    val orderId: Long,
    val shopId: Long,
    val customerName: String,
    val totalAmount: Long,
    val items: List<OrderTicketItemCommand>,
)
