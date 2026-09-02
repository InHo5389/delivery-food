package delivery.order.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class PaymentErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    INVALID_PAYMENT_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 결제 상태 전이입니다."),
    PAYMENT_DECLINED(HttpStatus.PAYMENT_REQUIRED, "결제가 승인되지 않았습니다."),
}
