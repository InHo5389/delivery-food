package delivery.order.application.dto

import java.time.LocalDate

data class SalesSummaryQuery(
    val shopId: Long,
    val date: LocalDate,
)
