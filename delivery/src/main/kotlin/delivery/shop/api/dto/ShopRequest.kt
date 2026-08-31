package delivery.shop.api.dto

import delivery.shop.application.dto.CreateShopCommand
import delivery.shop.application.dto.UpdateShopCommand
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class CreateShopRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val address: String,
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: BigDecimal,
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: BigDecimal,
    @field:NotBlank
    val phone: String,
    @field:PositiveOrZero
    val minOrderAmount: Long,
    @field:PositiveOrZero
    val deliveryFee: Long,
) {
    fun toCommand(): CreateShopCommand =
        CreateShopCommand(name, address, latitude, longitude, phone, minOrderAmount, deliveryFee)
}

data class UpdateShopRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val address: String,
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: BigDecimal,
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: BigDecimal,
    @field:NotBlank
    val phone: String,
    @field:PositiveOrZero
    val minOrderAmount: Long,
    @field:PositiveOrZero
    val deliveryFee: Long,
) {
    fun toCommand(): UpdateShopCommand =
        UpdateShopCommand(name, address, latitude, longitude, phone, minOrderAmount, deliveryFee)
}
