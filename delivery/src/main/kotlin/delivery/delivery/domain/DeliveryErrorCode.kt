package delivery.delivery.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class DeliveryErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배달을 찾을 수 없습니다."),
    INVALID_DELIVERY_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 배달 상태 전이입니다."),
}
