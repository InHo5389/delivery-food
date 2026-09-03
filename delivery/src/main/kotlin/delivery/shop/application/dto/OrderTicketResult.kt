package delivery.shop.application.dto

data class OrderTicketItemResult(
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
)

data class OrderTicketResult(
    val ticketId: Long,
    val orderId: Long,
    val shopId: Long,
    val customerName: String,
    val totalAmount: Long,
    val status: String,
    val items: List<OrderTicketItemResult>,
)
