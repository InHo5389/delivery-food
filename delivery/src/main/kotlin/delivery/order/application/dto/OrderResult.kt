package delivery.order.application.dto

import delivery.order.domain.Order
import delivery.order.domain.OrderItem
import delivery.order.domain.Payment

data class OrderResult(
    val order: Order,
    val items: List<OrderItem>,
    val payment: Payment,
) {
    val totalAmount: Long
        get() = items.sumOf { it.menuPrice * it.quantity }
}
