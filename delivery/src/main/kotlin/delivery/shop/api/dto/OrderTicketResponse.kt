package delivery.shop.api.dto

import delivery.shop.application.dto.OrderTicketItemResult
import delivery.shop.application.dto.OrderTicketResult

data class OrderTicketItemResponse(
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
) {
    companion object {
        fun from(item: OrderTicketItemResult): OrderTicketItemResponse =
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
        fun from(result: OrderTicketResult): OrderTicketResponse =
            OrderTicketResponse(
                ticketId = result.ticketId,
                orderId = result.orderId,
                status = result.status,
                customerName = result.customerName,
                totalAmount = result.totalAmount,
                items = result.items.map(OrderTicketItemResponse::from),
            )
    }
}

data class OrderTicketListResponse(
    val tickets: List<OrderTicketResponse>,
) {
    companion object {
        fun from(results: List<OrderTicketResult>): OrderTicketListResponse =
            OrderTicketListResponse(results.map(OrderTicketResponse::from))
    }
}
