package delivery.order.api.dto

import delivery.order.application.dto.OrderHistoryQuery
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class OrderHistoryRequest(
    @field:Min(1) @field:Max(100)
    val size: Int = 20,
    @field:Min(0)
    val page: Int = 0,
) {
    fun toQuery(customerId: Long): OrderHistoryQuery = OrderHistoryQuery(customerId, page, size)
}
