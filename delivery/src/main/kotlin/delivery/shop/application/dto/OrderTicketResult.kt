package delivery.shop.application.dto

data class OrderTicketResult(
    val ticketId: Long,
    val orderId: Long,
    val shopId: Long,
    val customerName: String,
    val totalAmount: Long,
    val status: String,
)
