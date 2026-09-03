package delivery.delivery.infrastructure

import java.time.Instant

data class RiderCandidateRow(
    val riderId: Long,
    val distanceMeters: Double,
    val recentDeliveryCount: Int,
    val acceptanceRate: Double,
    val availableSince: Instant?,
)
