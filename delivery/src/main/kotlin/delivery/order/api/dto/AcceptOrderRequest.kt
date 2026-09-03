package delivery.order.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class AcceptOrderRequest(
    @field:Min(1) @field:Max(120)
    val estimatedCookingMinutes: Int,
)
