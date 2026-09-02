package delivery.order.application.dto

data class OrderHistoryQuery(
    val customerId: Long,
    val page: Int,
    val size: Int,
)
