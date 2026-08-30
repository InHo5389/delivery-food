package delivery.search.application.dto

data class ShopKeywordSearchQuery(
    val keyword: String,
    val limit: Int,
    val offset: Int,
)

data class ShopKeywordSearchResult(
    val shopId: Long,
    val name: String,
    val address: String,
)
