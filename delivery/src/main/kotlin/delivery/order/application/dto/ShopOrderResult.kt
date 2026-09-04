package delivery.order.application.dto

data class ShopOrderResult(
    val orderId: Long,
    val status: String,
    val customerName: String,
    val totalAmount: Long,
    val items: List<OrderItemSummary>,
)
