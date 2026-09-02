package delivery.order.api.dto

import delivery.order.application.dto.OrderResult
import delivery.order.domain.Order

data class CreateOrderResponse(
    val orderIds: List<Long>,
    val status: String,
    val totalAmount: Long,
) {
    companion object {
        fun from(result: OrderResult): CreateOrderResponse =
            CreateOrderResponse(
                orderIds = result.orders.mapNotNull { it.id },
                status = result.orders.first().status.name,
                totalAmount = result.totalAmount,
            )
    }
}

data class OrderResponse(
    val orderId: Long,
    val shopId: Long,
    val menuId: Long,
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
    val status: String,
) {
    companion object {
        fun from(order: Order): OrderResponse =
            OrderResponse(
                orderId = order.id!!,
                shopId = order.shopId,
                menuId = order.menuId,
                menuName = order.menuName,
                menuPrice = order.menuPrice,
                quantity = order.quantity,
                status = order.status.name,
            )
    }
}
