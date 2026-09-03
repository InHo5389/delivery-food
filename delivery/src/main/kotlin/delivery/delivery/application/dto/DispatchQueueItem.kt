package delivery.delivery.application.dto

data class DispatchQueueItem(
    val deliveryId: Long,
    val orderId: Long,
    val shopId: Long,
)
