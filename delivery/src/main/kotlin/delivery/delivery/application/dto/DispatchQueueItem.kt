package delivery.delivery.application.dto

import java.time.Instant

data class DispatchQueueItem(
    val deliveryId: Long,
    val orderId: Long,
    val shopId: Long,
    val estimatedPickupAt: Instant?,
)
