package delivery.shop.api.dto

import delivery.shop.domain.Shop
import java.math.BigDecimal

data class ShopResponse(
    val shopId: Long,
    val ownerId: Long,
    val name: String,
    val address: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val phone: String,
    val minOrderAmount: Long,
    val deliveryFee: Long,
    val status: String,
) {
    companion object {
        fun from(shop: Shop): ShopResponse =
            ShopResponse(
                shopId = shop.id!!,
                ownerId = shop.ownerId,
                name = shop.name,
                address = shop.address,
                latitude = shop.latitude,
                longitude = shop.longitude,
                phone = shop.phone,
                minOrderAmount = shop.minOrderAmount,
                deliveryFee = shop.deliveryFee,
                status = shop.status.name,
            )
    }
}
