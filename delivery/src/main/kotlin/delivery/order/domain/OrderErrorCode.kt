package delivery.order.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class OrderErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 주문 상태 전이입니다."),
    EMPTY_CART(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다."),
    SHOP_NOT_OPEN(HttpStatus.CONFLICT, "지금은 주문을 받지 않는 상점입니다."),
    MENU_SOLD_OUT(HttpStatus.CONFLICT, "품절된 메뉴가 포함되어 있습니다."),
    MENU_PRICE_CHANGED(HttpStatus.CONFLICT, "메뉴 가격이 변경되었습니다. 장바구니를 다시 확인해주세요."),
    BELOW_MIN_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "최소주문금액을 채우지 못했습니다."),
}
