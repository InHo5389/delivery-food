package delivery.order.application.dto

import delivery.order.domain.Order
import delivery.order.domain.Payment

data class OrderResult(
    val orders: List<Order>,
    val payment: Payment,
) {
    val totalAmount: Long
        get() = orders.sumOf { it.menuPrice * it.quantity }
}
