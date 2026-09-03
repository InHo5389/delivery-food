package delivery.delivery.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class DeliveryErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배달을 찾을 수 없습니다."),
    INVALID_DELIVERY_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 배달 상태 전이입니다."),
    OFFER_NOT_FOUND(HttpStatus.NOT_FOUND, "배차 오퍼를 찾을 수 없습니다."),
    NOT_YOUR_OFFER(HttpStatus.FORBIDDEN, "본인에게 온 오퍼가 아닙니다."),
    DISPATCH_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "이미 다른 라이더가 배정된 배달입니다."),
    RIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "라이더 정보를 찾을 수 없습니다."),
}
