package delivery.shop.domain

enum class OrderTicketStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COOKING,
    COOKED,
    ;

    fun canTransitionTo(next: OrderTicketStatus): Boolean = next in allowedNextStatuses()

    private fun allowedNextStatuses(): Set<OrderTicketStatus> = when (this) {
        PENDING -> setOf(ACCEPTED, REJECTED)
        ACCEPTED -> setOf(COOKING)
        REJECTED -> emptySet()
        COOKING -> setOf(COOKED)
        COOKED -> emptySet()
    }
}
