package delivery.delivery.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class DispatchQueueRequest(
    @field:Min(1) @field:Max(100)
    val limit: Int = 20,
)
