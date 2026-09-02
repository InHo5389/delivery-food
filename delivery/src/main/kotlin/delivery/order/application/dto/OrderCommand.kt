package delivery.order.application.dto

data class CreateOrderCommand(
    val customerId: Long,
    val customerName: String,
    val customerPhone: String,
)
