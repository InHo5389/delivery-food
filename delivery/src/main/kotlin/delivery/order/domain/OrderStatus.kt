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

    // 조리 진행(ACCEPTED→COOKING→COOKED)과 라이더 배정(RIDER_ASSIGNED)은 서로 독립적으로
    // 진행된다 — 배차는 접수 시점에 바로 시작되므로, 라이더가 조리 완료 *전에* 오퍼를
    // 수락하는 경우가 오히려 일반적이다(조리 시간에 맞춰 도착하게 하는 게 목적).
    // 그래서 RIDER_ASSIGNED는 ACCEPTED/COOKING/COOKED 어디서든 도달할 수 있고, 반대로
    // RIDER_ASSIGNED에 먼저 도달했더라도 조리 진행(COOKING/COOKED)은 계속 이어질 수 있다.
    // "픽업하려면 조리와 배정이 둘 다 끝나야 한다"는 실제 제약은 라이더가 물리적으로
    // 음식을 받아야 픽업을 누르는 현실 흐름과, Delivery 쪽 상태머신(ASSIGNED에서만
    // PICKED_UP 가능)이 이미 강제하고 있어 Order.status에서 다시 강제하지 않는다.
    private fun allowedNextStatuses(): Set<OrderStatus> = when (this) {
        CREATED -> setOf(PAID, PAYMENT_FAILED, CANCELLED)
        PAID -> setOf(ACCEPTED, REJECTED, CANCELLED)
        PAYMENT_FAILED -> emptySet()
        // ACCEPTED 이후로는 고객의 자유 취소를 허용하지 않는다 (가이드 문서 커밋 39).
        ACCEPTED -> setOf(COOKING, RIDER_ASSIGNED)
        REJECTED -> emptySet()
        COOKING -> setOf(COOKED, RIDER_ASSIGNED)
        COOKED -> setOf(RIDER_ASSIGNED, PICKED_UP)
        RIDER_ASSIGNED -> setOf(COOKING, COOKED, PICKED_UP)
        PICKED_UP -> setOf(DELIVERED)
        DELIVERED -> emptySet()
        CANCELLED -> emptySet()
    }
}
