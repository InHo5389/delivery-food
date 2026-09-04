package delivery.coupon.application.dto

import java.time.Instant

data class CreateCouponCommand(
    val name: String,
    val shopId: Long?,
    val totalQuantity: Int,
    val validityDays: Int,
    val startsAt: Instant?,
)
