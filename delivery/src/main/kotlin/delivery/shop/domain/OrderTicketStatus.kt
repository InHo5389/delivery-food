package delivery.shop.domain

enum class OrderTicketStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COOKING,
    COOKED,
    // 고객이 취소하거나(주문 접수 전 자유 취소) 사장님이 3분 안에 반응하지 않아
    // 스케줄러가 대신 취소한 경우. Order 쪽 상태머신상 PAID에서만 CANCELLED로 갈 수
    // 있고 PAID 시점에만 티켓이 존재하므로, 이 전이는 항상 PENDING에서만 일어난다.
    CANCELLED,
    ;

    fun canTransitionTo(next: OrderTicketStatus): Boolean = next in allowedNextStatuses()

    private fun allowedNextStatuses(): Set<OrderTicketStatus> = when (this) {
        PENDING -> setOf(ACCEPTED, REJECTED, CANCELLED)
        ACCEPTED -> setOf(COOKING)
        REJECTED -> emptySet()
        COOKING -> setOf(COOKED)
        COOKED -> emptySet()
        CANCELLED -> emptySet()
    }
}
