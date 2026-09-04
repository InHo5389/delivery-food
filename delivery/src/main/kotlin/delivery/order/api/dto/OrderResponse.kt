package delivery.order.api.dto

import delivery.order.application.dto.OrderHistoryItem
import delivery.order.application.dto.OrderResult
import delivery.order.domain.Order
import delivery.order.domain.OrderItem

data class OrderItemResponse(
    val menuId: Long,
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
) {
    companion object {
        fun from(item: OrderItem): OrderItemResponse =
            OrderItemResponse(item.menuId, item.menuName, item.menuPrice, item.quantity)
    }
}

data class CreateOrderResponse(
    val orderId: Long,
    val status: String,
    val totalAmount: Long,
    val deliveryFee: Long,
    val items: List<OrderItemResponse>,
) {
    companion object {
        fun from(result: OrderResult): CreateOrderResponse =
            CreateOrderResponse(
                orderId = result.order.id!!,
                status = result.order.status.name,
                totalAmount = result.totalAmount,
                deliveryFee = result.order.deliveryFee,
                items = result.items.map(OrderItemResponse::from),
            )
    }
}

data class OrderResponse(
    val orderId: Long,
    val shopId: Long,
    val status: String,
    val deliveryFee: Long,
    val items: List<OrderItemResponse>,
) {
    companion object {
        fun from(order: Order, items: List<OrderItem>): OrderResponse =
            OrderResponse(
                orderId = order.id!!,
                shopId = order.shopId,
                status = order.status.name,
                deliveryFee = order.deliveryFee,
                items = items.map(OrderItemResponse::from),
            )

        fun from(historyItem: OrderHistoryItem): OrderResponse = from(historyItem.order, historyItem.items)
    }
}
