package delivery.delivery.infrastructure

import java.time.Instant

data class DispatchQueueRow(
    val deliveryId: Long,
    val orderId: Long,
    val shopId: Long,
    val estimatedPickupAt: Instant?,
)
