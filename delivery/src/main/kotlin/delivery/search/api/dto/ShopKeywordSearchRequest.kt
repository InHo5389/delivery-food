package delivery.search.api.dto

import delivery.search.application.dto.ShopKeywordSearchQuery
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ShopKeywordSearchRequest(
    @field:NotBlank
    val keyword: String?,
    @field:Min(1) @field:Max(100)
    val size: Int = 20,
    @field:Min(0)
    val page: Int = 0,
) {
    fun toQuery(): ShopKeywordSearchQuery =
        ShopKeywordSearchQuery(
            keyword = keyword!!,
            limit = size,
            offset = page * size,
        )
}
