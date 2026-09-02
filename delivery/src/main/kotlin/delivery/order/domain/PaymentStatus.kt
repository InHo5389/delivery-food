package delivery.order.domain

enum class PaymentStatus {
    READY,
    AUTH,
    APPROVED,
    FAILED,
    ;

    // ★ OrderStatus와 동일하게 when exhaustive 체크로 상태 전이를 방어한다(커밋 33 참조).
    fun canTransitionTo(next: PaymentStatus): Boolean = next in allowedNextStatuses()

    private fun allowedNextStatuses(): Set<PaymentStatus> = when (this) {
        READY -> setOf(AUTH, FAILED)
        AUTH -> setOf(APPROVED, FAILED)
        APPROVED -> emptySet()
        FAILED -> emptySet()
    }
}
