package delivery.shop.api.dto

import delivery.shop.domain.OwnerProfile

data class OwnerProfileResponse(
    val ownerProfileId: Long,
    val businessRegistrationNumber: String,
    val businessName: String,
    val settlementBank: String,
    val settlementAccount: String,
) {
    companion object {
        fun from(profile: OwnerProfile): OwnerProfileResponse =
            OwnerProfileResponse(
                ownerProfileId = profile.id!!,
                businessRegistrationNumber = profile.businessRegistrationNumber,
                businessName = profile.businessName,
                settlementBank = profile.settlementBank,
                settlementAccount = profile.settlementAccount,
            )
    }
}
