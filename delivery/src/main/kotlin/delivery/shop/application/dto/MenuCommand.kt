package delivery.shop.application.dto

data class CreateMenuGroupCommand(
    val shopId: Long,
    val name: String,
    val displayOrder: Int,
)

data class CreateMenuCommand(
    val shopId: Long,
    val menuGroupId: Long,
    val name: String,
    val description: String?,
    val price: Long,
    val displayOrder: Int,
)

data class UpdateMenuCommand(
    val name: String,
    val description: String?,
    val price: Long,
    val displayOrder: Int,
)
