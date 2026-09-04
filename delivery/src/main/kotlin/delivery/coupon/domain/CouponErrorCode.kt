package delivery.coupon.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class CouponErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    INVALID_ISSUANCE_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 발급 상태 전이입니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    NOT_STARTED(HttpStatus.CONFLICT, "아직 발급을 시작하지 않은 쿠폰입니다."),
    SOLD_OUT(HttpStatus.CONFLICT, "쿠폰이 매진되었습니다."),
    ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "본인 소유가 아닙니다."),
    SHOP_ID_REQUIRED(HttpStatus.BAD_REQUEST, "사장님은 쿠폰을 발행할 상점의 shopId가 필요합니다."),
}
