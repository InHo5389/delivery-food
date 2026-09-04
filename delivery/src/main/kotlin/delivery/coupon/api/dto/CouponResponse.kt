package delivery.coupon.api.dto

import delivery.coupon.domain.Coupon
import delivery.coupon.domain.Issuance
import java.time.Instant

data class CouponResponse(
    val id: Long,
    val name: String,
    val shopId: Long?,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val validityDays: Int,
    val startsAt: Instant?,
) {
    companion object {
        fun from(coupon: Coupon): CouponResponse =
            CouponResponse(coupon.id!!, coupon.name, coupon.shopId, coupon.totalQuantity, coupon.issuedQuantity, coupon.validityDays, coupon.startsAt)
    }
}

data class IssuanceResponse(
    val id: Long,
    val couponId: Long,
    val status: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant?,
) {
    companion object {
        fun from(issuance: Issuance): IssuanceResponse =
            IssuanceResponse(issuance.id!!, issuance.couponId, issuance.status.name, issuance.issuedAt, issuance.expiresAt, issuance.usedAt)
    }
}

data class IssuanceListResponse(
    val issuances: List<IssuanceResponse>,
)
