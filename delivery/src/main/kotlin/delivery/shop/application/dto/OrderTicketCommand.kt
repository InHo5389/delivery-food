package delivery.shop.application.dto

data class CreateOrderTicketCommand(
    val orderId: Long,
    val shopId: Long,
    val customerName: String,
    val totalAmount: Long,
)
