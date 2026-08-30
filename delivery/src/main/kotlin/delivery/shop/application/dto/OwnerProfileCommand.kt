package delivery.shop.application.dto

data class CreateOwnerProfileCommand(
    val accountId: Long,
    val businessRegistrationNumber: String,
    val businessName: String,
    val settlementBank: String,
    val settlementAccount: String,
)

data class UpdateOwnerProfileCommand(
    val businessRegistrationNumber: String,
    val businessName: String,
    val settlementBank: String,
    val settlementAccount: String,
)
