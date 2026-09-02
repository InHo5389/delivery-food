package delivery.order.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class CartErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."),
    DIFFERENT_SHOP_IN_CART(HttpStatus.CONFLICT, "다른 상점의 메뉴는 함께 담을 수 없습니다. 장바구니를 비우고 다시 담아주세요."),
    INVALID_CART_ITEM_QUANTITY(HttpStatus.BAD_REQUEST, "수량은 1개 이상이어야 합니다."),
}
