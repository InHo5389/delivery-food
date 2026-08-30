package delivery.shop.domain

import delivery.common.exception.ErrorCode
import org.springframework.http.HttpStatus

enum class ShopErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    SHOP_NOT_FOUND(HttpStatus.NOT_FOUND, "상점을 찾을 수 없습니다."),
    MENU_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴 그룹을 찾을 수 없습니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    MENU_OPTION_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴 옵션 그룹을 찾을 수 없습니다."),
    MENU_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴 옵션을 찾을 수 없습니다."),
}
