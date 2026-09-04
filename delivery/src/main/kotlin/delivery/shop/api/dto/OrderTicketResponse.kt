package delivery.shop.api.dto

import delivery.order.application.dto.OrderItemSummary
import delivery.shop.application.dto.OrderTicketResult

data class OrderTicketItemResponse(
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
) {
    companion object {
        fun from(item: OrderItemSummary): OrderTicketItemResponse =
            OrderTicketItemResponse(item.menuName, item.menuPrice, item.quantity)
    }
}

data class OrderTicketResponse(
    val ticketId: Long,
    val orderId: Long,
    val status: String,
    val customerName: String,
    val totalAmount: Long,
    val items: List<OrderTicketItemResponse>,
) {
    companion object {
        fun from(result: OrderTicketResult, items: List<OrderItemSummary>): OrderTicketResponse =
            OrderTicketResponse(
                ticketId = result.ticketId,
                orderId = result.orderId,
                status = result.status,
                customerName = result.customerName,
                totalAmount = result.totalAmount,
                items = items.map(OrderTicketItemResponse::from),
            )
    }
}

data class OrderTicketListResponse(
    val tickets: List<OrderTicketResponse>,
)
