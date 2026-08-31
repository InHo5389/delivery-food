package delivery.shop.api.dto

import delivery.shop.domain.Menu

data class MenuCreateResponse(
    val menuId: Long,
    val shopId: Long,
    val menuGroupId: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val soldOut: Boolean,
    val displayOrder: Int,
) {
    companion object {
        fun from(menu: Menu): MenuCreateResponse =
            MenuCreateResponse(
                menuId = menu.id!!,
                shopId = menu.shopId,
                menuGroupId = menu.menuGroupId,
                name = menu.name,
                description = menu.description,
                price = menu.price,
                soldOut = menu.soldOut,
                displayOrder = menu.displayOrder,
            )
    }
}
