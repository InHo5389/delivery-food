package delivery.order.api.dto

import delivery.order.application.dto.OrderItemSummary
import delivery.order.application.dto.ShopOrderResult

data class ShopOrderItemResponse(
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
) {
    companion object {
        fun from(item: OrderItemSummary): ShopOrderItemResponse =
            ShopOrderItemResponse(item.menuName, item.menuPrice, item.quantity)
    }
}

data class ShopOrderResponse(
    val orderId: Long,
    val status: String,
    val customerName: String,
    val totalAmount: Long,
    val items: List<ShopOrderItemResponse>,
) {
    companion object {
        fun from(result: ShopOrderResult): ShopOrderResponse =
            ShopOrderResponse(
                orderId = result.orderId,
                status = result.status,
                customerName = result.customerName,
                totalAmount = result.totalAmount,
                items = result.items.map(ShopOrderItemResponse::from),
            )
    }
}

data class ShopOrderListResponse(
    val tickets: List<ShopOrderResponse>,
)
