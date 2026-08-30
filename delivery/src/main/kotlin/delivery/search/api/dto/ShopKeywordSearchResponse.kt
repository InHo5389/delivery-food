package delivery.search.api.dto

import delivery.search.application.dto.ShopKeywordSearchResult

data class ShopKeywordSearchResponse(
    val shopId: Long,
    val name: String,
    val address: String,
) {
    companion object {
        fun from(result: ShopKeywordSearchResult): ShopKeywordSearchResponse =
            ShopKeywordSearchResponse(result.shopId, result.name, result.address)
    }
}
