package delivery.search.infrastructure

data class ShopKeywordSearchRow(
    val id: Long,
    val name: String,
    val address: String,
    val minOrderAmount: Long,
    val deliveryFee: Long,
)
