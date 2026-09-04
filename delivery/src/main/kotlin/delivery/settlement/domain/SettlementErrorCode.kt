package delivery.settlement.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class SettlementErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 내역을 찾을 수 없습니다."),
    SETTLEMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 같은 기간의 정산이 존재합니다."),
    INVALID_SETTLEMENT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 정산 상태 전이입니다."),
    COMMISSION_RATE_NOT_FOUND(HttpStatus.NOT_FOUND, "적용할 수수료율을 찾을 수 없습니다."),
}
