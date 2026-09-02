package delivery.order.application.dto

import delivery.order.domain.Order
import delivery.order.domain.OrderItem

data class OrderHistoryItem(
    val order: Order,
    val items: List<OrderItem>,
)

data class OrderHistoryResult(
    val orders: List<OrderHistoryItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
