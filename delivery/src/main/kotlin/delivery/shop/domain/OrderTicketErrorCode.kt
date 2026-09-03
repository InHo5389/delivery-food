package delivery.shop.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class OrderTicketErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    ORDER_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "주문 티켓을 찾을 수 없습니다."),
    INVALID_ORDER_TICKET_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 주문 티켓 상태 전이입니다."),
}
