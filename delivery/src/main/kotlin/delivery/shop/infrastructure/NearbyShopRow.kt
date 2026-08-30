package delivery.shop.infrastructure

data class NearbyShopRow(
    val id: Long,
    val name: String,
    val address: String,
    val distanceMeters: Double,
)
