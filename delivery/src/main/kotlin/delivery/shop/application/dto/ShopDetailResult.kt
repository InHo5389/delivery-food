package delivery.shop.application.dto

import delivery.shop.domain.BusinessHours
import delivery.shop.domain.Menu
import delivery.shop.domain.MenuGroup
import delivery.shop.domain.Shop

data class ShopDetailResult(
    val shop: Shop,
    val businessHours: List<BusinessHours>,
    val menuGroups: List<MenuGroupResult>,
)

data class MenuGroupResult(
    val menuGroup: MenuGroup,
    val menus: List<Menu>,
)
