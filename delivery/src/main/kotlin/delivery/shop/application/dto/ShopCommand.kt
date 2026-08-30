package delivery.shop.application.dto

import java.math.BigDecimal

data class CreateShopCommand(
    val ownerId: Long,
    val name: String,
    val address: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val phone: String,
)

data class UpdateShopCommand(
    val name: String,
    val address: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val phone: String,
)
