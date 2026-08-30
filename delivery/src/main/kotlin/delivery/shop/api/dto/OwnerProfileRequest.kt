package delivery.shop.api.dto

import delivery.shop.application.dto.CreateOwnerProfileCommand
import delivery.shop.application.dto.UpdateOwnerProfileCommand
import jakarta.validation.constraints.NotBlank

data class CreateOwnerProfileRequest(
    @field:NotBlank
    val businessRegistrationNumber: String,
    @field:NotBlank
    val businessName: String,
    @field:NotBlank
    val settlementBank: String,
    @field:NotBlank
    val settlementAccount: String,
) {
    fun toCommand(accountId: Long): CreateOwnerProfileCommand =
        CreateOwnerProfileCommand(accountId, businessRegistrationNumber, businessName, settlementBank, settlementAccount)
}

data class UpdateOwnerProfileRequest(
    @field:NotBlank
    val businessRegistrationNumber: String,
    @field:NotBlank
    val businessName: String,
    @field:NotBlank
    val settlementBank: String,
    @field:NotBlank
    val settlementAccount: String,
) {
    fun toCommand(): UpdateOwnerProfileCommand =
        UpdateOwnerProfileCommand(businessRegistrationNumber, businessName, settlementBank, settlementAccount)
}
