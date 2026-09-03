package delivery.order.api.dto

import delivery.order.application.dto.SalesSummaryResult
import java.time.LocalDate

data class SalesSummaryResponse(
    val date: LocalDate,
    val orderCount: Long,
    val totalAmount: Long,
) {
    companion object {
        fun from(result: SalesSummaryResult): SalesSummaryResponse =
            SalesSummaryResponse(result.date, result.orderCount, result.totalAmount)
    }
}
