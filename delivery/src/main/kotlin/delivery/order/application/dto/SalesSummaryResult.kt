package delivery.order.application.dto

import java.time.LocalDate

data class SalesSummaryResult(
    val date: LocalDate,
    val orderCount: Long,
    val totalAmount: Long,
)
