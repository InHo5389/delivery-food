package delivery.shop.application.dto

import java.math.BigDecimal

data class CreateShopCommand(
    val name: String,
    val address: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val phone: String,
    val minOrderAmount: Long,
    val deliveryFee: Long,
)

data class UpdateShopCommand(
    val name: String,
    val address: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val phone: String,
    val minOrderAmount: Long,
    val deliveryFee: Long,
)
