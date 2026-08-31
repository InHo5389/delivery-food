package delivery.shop.api.dto

import delivery.shop.application.dto.CreateMenuGroupCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class CreateMenuGroupRequest(
    @field:NotBlank
    val name: String,
    @field:Min(0)
    val displayOrder: Int,
) {
    fun toCommand(shopId: Long): CreateMenuGroupCommand = CreateMenuGroupCommand(shopId, name, displayOrder)
}
