package delivery.shop.api.dto

import delivery.shop.domain.MenuGroup

data class MenuGroupCreateResponse(
    val menuGroupId: Long,
    val shopId: Long,
    val name: String,
    val displayOrder: Int,
) {
    companion object {
        fun from(menuGroup: MenuGroup): MenuGroupCreateResponse =
            MenuGroupCreateResponse(menuGroup.id!!, menuGroup.shopId, menuGroup.name, menuGroup.displayOrder)
    }
}
