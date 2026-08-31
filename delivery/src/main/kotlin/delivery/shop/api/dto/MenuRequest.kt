package delivery.shop.api.dto

import delivery.shop.application.dto.BulkCreateMenuCommand
import delivery.shop.application.dto.CreateMenuCommand
import delivery.shop.application.dto.CreateMenuItem
import delivery.shop.application.dto.UpdateMenuCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class CreateMenuRequest(
    val menuGroupId: Long,
    @field:NotBlank
    val name: String,
    val description: String?,
    @field:Positive
    val price: Long,
    @field:Min(0)
    val displayOrder: Int,
) {
    fun toCommand(shopId: Long): CreateMenuCommand =
        CreateMenuCommand(shopId, menuGroupId, name, description, price, displayOrder)
}

data class BulkCreateMenuItemRequest(
    val menuGroupId: Long,
    @field:NotBlank
    val name: String,
    val description: String?,
    @field:Positive
    val price: Long,
    @field:Min(0)
    val displayOrder: Int,
)

data class BulkCreateMenuRequest(
    @field:NotEmpty
    @field:Valid
    val menus: List<BulkCreateMenuItemRequest>,
) {
    fun toCommand(shopId: Long): BulkCreateMenuCommand =
        BulkCreateMenuCommand(
            shopId = shopId,
            menus = menus.map { CreateMenuItem(it.menuGroupId, it.name, it.description, it.price, it.displayOrder) },
        )
}

data class UpdateMenuRequest(
    @field:NotBlank
    val name: String,
    val description: String?,
    @field:Positive
    val price: Long,
    @field:Min(0)
    val displayOrder: Int,
) {
    fun toCommand(): UpdateMenuCommand = UpdateMenuCommand(name, description, price, displayOrder)
}
