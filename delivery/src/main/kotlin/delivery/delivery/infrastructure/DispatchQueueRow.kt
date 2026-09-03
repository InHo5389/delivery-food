package delivery.delivery.infrastructure

data class DispatchQueueRow(
    val deliveryId: Long,
    val orderId: Long,
    val shopId: Long,
)
