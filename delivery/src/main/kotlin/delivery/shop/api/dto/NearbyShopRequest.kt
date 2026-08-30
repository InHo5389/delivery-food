package delivery.shop.api.dto

import delivery.shop.application.dto.NearbyShopQuery
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class NearbyShopRequest(
    @field:NotNull
    val latitude: Double?,
    @field:NotNull
    val longitude: Double?,
    @field:Min(1) @field:Max(100)
    val size: Int = 20,
    @field:Min(0)
    val page: Int = 0,
) {
    fun toQuery(): NearbyShopQuery =
        NearbyShopQuery(
            latitude = latitude!!,
            longitude = longitude!!,
            limit = size,
            offset = page * size,
        )
}
