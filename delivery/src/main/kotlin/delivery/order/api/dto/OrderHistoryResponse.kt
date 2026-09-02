package delivery.order.api.dto

import delivery.order.application.dto.OrderHistoryResult

data class OrderHistoryResponse(
    val orders: List<OrderResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(result: OrderHistoryResult): OrderHistoryResponse =
            OrderHistoryResponse(
                orders = result.orders.map { OrderResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
    }
}
