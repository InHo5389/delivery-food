package delivery.coupon.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class CouponErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    INVALID_ISSUANCE_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 발급 상태 전이입니다."),
}
