package delivery.order.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class OrderErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 주문 상태 전이입니다."),
}
