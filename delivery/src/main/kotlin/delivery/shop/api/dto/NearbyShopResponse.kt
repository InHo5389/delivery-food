package delivery.shop.api.dto

import delivery.shop.application.dto.NearbyShopResult

data class NearbyShopResponse(
    val shopId: Long,
    val name: String,
    val address: String,
    val distanceMeters: Double,
) {
    companion object {
        fun from(result: NearbyShopResult): NearbyShopResponse =
            NearbyShopResponse(result.shopId, result.name, result.address, result.distanceMeters)
    }
}
