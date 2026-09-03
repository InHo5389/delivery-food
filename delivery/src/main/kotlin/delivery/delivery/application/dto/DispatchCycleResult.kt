package delivery.delivery.application.dto

data class DispatchCycleResult(
    val deliveryId: Long,
    val offeredRiderIds: List<Long>,
)
