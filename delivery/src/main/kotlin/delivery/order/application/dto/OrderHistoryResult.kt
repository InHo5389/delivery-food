package delivery.order.application.dto

import delivery.order.domain.Order

data class OrderHistoryResult(
    val orders: List<Order>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
