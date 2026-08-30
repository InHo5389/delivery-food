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
    MENU_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴 이미지를 찾을 수 없습니다."),
    INVALID_MENU_IMAGE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 파일입니다."),
}
