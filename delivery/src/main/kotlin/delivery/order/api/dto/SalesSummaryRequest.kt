package delivery.order.api.dto

import delivery.order.application.dto.SalesSummaryQuery
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class SalesSummaryRequest(
    @field:NotNull
    val shopId: Long?,
    @field:NotNull
    val date: LocalDate?,
) {
    fun toQuery(): SalesSummaryQuery = SalesSummaryQuery(shopId = shopId!!, date = date!!)
}
