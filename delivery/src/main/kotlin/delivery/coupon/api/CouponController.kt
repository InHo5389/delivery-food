package delivery.coupon.api

import delivery.common.security.AuthenticatedUser
import delivery.coupon.api.dto.CouponResponse
import delivery.coupon.api.dto.CreateCouponRequest
import delivery.coupon.api.dto.IssuanceListResponse
import delivery.coupon.api.dto.IssuanceResponse
import delivery.coupon.application.CouponService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponController(
    private val couponService: CouponService,
) {
    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCoupon(
        @Valid @RequestBody request: CreateCouponRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): CouponResponse = CouponResponse.from(couponService.createCoupon(requester, request.toCommand()))

    @PostMapping("/coupons/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    fun issue(
        @PathVariable couponId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): IssuanceResponse = IssuanceResponse.from(couponService.issue(couponId, requester.userId))

    @GetMapping("/users/me/issuances")
    fun getMyIssuances(
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): IssuanceListResponse = IssuanceListResponse(couponService.getMyIssuances(requester.userId).map(IssuanceResponse::from))
}
