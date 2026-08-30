package delivery.shop.api.dto

import delivery.shop.application.dto.ShopDetailResult
import java.time.DayOfWeek
import java.time.LocalTime

data class ShopDetailResponse(
    val shopId: Long,
    val name: String,
    val address: String,
    val phone: String,
    val status: String,
    val businessHours: List<BusinessHoursResponse>,
    val menuGroups: List<MenuGroupResponse>,
) {
    companion object {
        fun from(result: ShopDetailResult): ShopDetailResponse =
            ShopDetailResponse(
                shopId = result.shop.id!!,
                name = result.shop.name,
                address = result.shop.address,
                phone = result.shop.phone,
                status = result.shop.status.name,
                businessHours = result.businessHours.map {
                    BusinessHoursResponse(it.dayOfWeek, it.openTime, it.closeTime)
                },
                menuGroups = result.menuGroups.map { group ->
                    MenuGroupResponse(
                        menuGroupId = group.menuGroup.id!!,
                        name = group.menuGroup.name,
                        menus = group.menus.map {
                            MenuResponse(
                                menuId = it.id!!,
                                name = it.name,
                                description = it.description,
                                price = it.price,
                                soldOut = it.soldOut,
                            )
                        },
                    )
                },
            )
    }
}

data class BusinessHoursResponse(
    val dayOfWeek: DayOfWeek,
    val openTime: LocalTime,
    val closeTime: LocalTime,
)

data class MenuGroupResponse(
    val menuGroupId: Long,
    val name: String,
    val menus: List<MenuResponse>,
)

data class MenuResponse(
    val menuId: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val soldOut: Boolean,
)
