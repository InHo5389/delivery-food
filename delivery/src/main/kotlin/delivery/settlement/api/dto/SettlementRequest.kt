package delivery.settlement.api.dto

import delivery.settlement.application.dto.AdminSettlementRangeQuery
import delivery.settlement.application.dto.MySettlementQuery
import jakarta.validation.constraints.NotBlank

data class MySettlementRequest(
    @field:NotBlank
    val yearMonth: String?,
    val shopId: Long?,
) {
    fun toQuery(): MySettlementQuery = MySettlementQuery(yearMonth!!, shopId)
}

data class AdminSettlementListRequest(
    @field:NotBlank
    val from: String?,
    @field:NotBlank
    val to: String?,
) {
    fun toQuery(): AdminSettlementRangeQuery = AdminSettlementRangeQuery(from!!, to!!)
}
