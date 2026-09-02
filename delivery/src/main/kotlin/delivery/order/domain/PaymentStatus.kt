package delivery.order.domain

enum class PaymentStatus {
    READY,
    AUTH,
    APPROVED,
    FAILED,
    REFUNDED,
    ;

    // ★ OrderStatus와 동일하게 when exhaustive 체크로 상태 전이를 방어한다(커밋 33 참조).
    fun canTransitionTo(next: PaymentStatus): Boolean = next in allowedNextStatuses()

    private fun allowedNextStatuses(): Set<PaymentStatus> = when (this) {
        READY -> setOf(AUTH, FAILED)
        AUTH -> setOf(APPROVED, FAILED)
        // 승인된 결제만 환불할 수 있다 — 주문 취소(커밋 39)는 PAID 상태에서만 가능하므로
        // 이 시점의 결제는 항상 APPROVED다.
        APPROVED -> setOf(REFUNDED)
        FAILED -> emptySet()
        REFUNDED -> emptySet()
    }
}
