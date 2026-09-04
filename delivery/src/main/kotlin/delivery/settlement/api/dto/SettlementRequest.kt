package delivery.settlement.api.dto

import delivery.settlement.application.dto.AdminSettlementRangeQuery
import delivery.settlement.application.dto.MySettlementQuery
import jakarta.validation.constraints.NotBlank

data class MySettlementRequest(
    // 라이더는 정확히 그 날짜, 사장님은 그 날짜가 속한 주(월~일)로 해석된다
    // (SettlementQueryService.getMySettlement 참조).
    @field:NotBlank
    val date: String?,
    val shopId: Long?,
) {
    fun toQuery(): MySettlementQuery = MySettlementQuery(date!!, shopId)
}

data class AdminSettlementListRequest(
    @field:NotBlank
    val from: String?,
    @field:NotBlank
    val to: String?,
) {
    fun toQuery(): AdminSettlementRangeQuery = AdminSettlementRangeQuery(from!!, to!!)
}
