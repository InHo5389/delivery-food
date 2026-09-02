package delivery.order.domain

enum class OrderStatus {
    CREATED,
    PAID,
    PAYMENT_FAILED,
    ACCEPTED,
    REJECTED,
    COOKING,
    COOKED,
    RIDER_ASSIGNED,
    PICKED_UP,
    DELIVERED,
    CANCELLED,
    ;

    // ★ when으로 분기하면 새 OrderStatus를 추가했을 때 이 함수의 when이 컴파일 경고 없이
    //   빠짐없이(exhaustive) 처리되었는지 컴파일러가 강제한다. Map 기반보다 이 점에서 안전하다.
    fun canTransitionTo(next: OrderStatus): Boolean = next in allowedNextStatuses()

    private fun allowedNextStatuses(): Set<OrderStatus> = when (this) {
        CREATED -> setOf(PAID, PAYMENT_FAILED, CANCELLED)
        PAID -> setOf(ACCEPTED, REJECTED, CANCELLED)
        PAYMENT_FAILED -> emptySet()
        // ACCEPTED 이후로는 고객의 자유 취소를 허용하지 않는다 (가이드 문서 커밋 39).
        ACCEPTED -> setOf(COOKING)
        REJECTED -> emptySet()
        COOKING -> setOf(COOKED)
        COOKED -> setOf(RIDER_ASSIGNED)
        RIDER_ASSIGNED -> setOf(PICKED_UP)
        PICKED_UP -> setOf(DELIVERED)
        DELIVERED -> emptySet()
        CANCELLED -> emptySet()
    }
}
