package delivery.settlement.application.dto

data class MySettlementQuery(
    val date: String,
    val shopId: Long?,
)

data class AdminSettlementRangeQuery(
    val from: String,
    val to: String,
)
