package delivery.order.application.dto

data class RequestPaymentCommand(
    val orderId: Long,
    val amount: Long,
)
