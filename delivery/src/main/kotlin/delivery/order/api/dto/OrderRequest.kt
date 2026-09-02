package delivery.order.api.dto

import delivery.order.application.dto.CreateOrderCommand
import jakarta.validation.constraints.NotBlank

data class CreateOrderRequest(
    @field:NotBlank
    val customerName: String,
    @field:NotBlank
    val customerPhone: String,
) {
    fun toCommand(customerId: Long): CreateOrderCommand =
        CreateOrderCommand(customerId, customerName, customerPhone)
}
