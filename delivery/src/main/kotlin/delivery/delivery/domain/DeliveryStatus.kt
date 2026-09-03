package delivery.delivery.domain

enum class DeliveryStatus {
    PENDING,
    OFFERING,
    ASSIGNED,
    PICKED_UP,
    DELIVERED,
    CANCELLED,
    FAILED,
    ;

    // OrderStatus와 동일한 이유로 when 표현식을 쓴다 — 새 상태가 추가되면 컴파일러가
    // 이 when의 exhaustiveness를 강제해 전이 규칙 누락을 잡아준다.
    fun canTransitionTo(next: DeliveryStatus): Boolean = next in allowedNextStatuses()

    private fun allowedNextStatuses(): Set<DeliveryStatus> = when (this) {
        PENDING -> setOf(OFFERING, CANCELLED)
        // 오퍼 발송 후 라이더가 수락하면 ASSIGNED, 반복 무응답/거절이면 FAILED.
        OFFERING -> setOf(ASSIGNED, FAILED, CANCELLED)
        ASSIGNED -> setOf(PICKED_UP, CANCELLED)
        PICKED_UP -> setOf(DELIVERED)
        DELIVERED -> emptySet()
        CANCELLED -> emptySet()
        FAILED -> emptySet()
    }
}
