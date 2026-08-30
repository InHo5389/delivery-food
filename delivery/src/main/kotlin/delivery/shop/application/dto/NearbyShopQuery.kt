package delivery.shop.application.dto

data class NearbyShopQuery(
    val latitude: Double,
    val longitude: Double,
    val limit: Int,
    val offset: Int,
)

data class NearbyShopResult(
    val shopId: Long,
    val name: String,
    val address: String,
    val distanceMeters: Double,
)
