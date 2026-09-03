package delivery.delivery.application.dto

import java.math.BigDecimal

data class CreateDeliveryCommand(
    val orderId: Long,
    val shopId: Long,
    val pickupLatitude: BigDecimal,
    val pickupLongitude: BigDecimal,
    val estimatedCookingMinutes: Int,
)
