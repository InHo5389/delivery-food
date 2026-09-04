package delivery.coupon.api.dto

import delivery.coupon.application.dto.CreateCouponCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.Instant

data class CreateCouponRequest(
    @field:NotBlank
    val name: String?,
    val shopId: Long?,
    @field:NotNull
    @field:Positive
    val totalQuantity: Int?,
    @field:NotNull
    @field:Positive
    val validityDays: Int?,
    val startsAt: Instant?,
) {
    fun toCommand(): CreateCouponCommand = CreateCouponCommand(name!!, shopId, totalQuantity!!, validityDays!!, startsAt)
}
