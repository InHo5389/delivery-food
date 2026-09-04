package delivery.coupon.domain

enum class IssuanceStatus {
    ISSUED,
    USED,
    EXPIRED,
    ;

    // 다른 상태머신들과 동일한 이유로 when을 쓴다 — 새 상태가 추가되면 컴파일러가
    // 이 when의 exhaustiveness를 강제해 전이 규칙 누락을 잡아준다.
    fun canTransitionTo(next: IssuanceStatus): Boolean = next in allowedNextStatuses()

    private fun allowedNextStatuses(): Set<IssuanceStatus> = when (this) {
        ISSUED -> setOf(USED, EXPIRED)
        USED -> emptySet()
        EXPIRED -> emptySet()
    }
}
