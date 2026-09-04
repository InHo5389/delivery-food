package delivery.settlement.domain

import delivery.common.exception.BusinessException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// target_id는 targetType에 따라 shop 모듈의 Shop.id 또는 delivery 모듈의 Rider.id를 가리키는
// 논리 참조다 — 대상이 둘 중 하나로 갈리는 다형적 참조라 애초에 단일 FK로 표현할 수 없고,
// 모듈 경계 규칙상으로도 다른 모듈 테이블과 FK는 금지된다.
@Entity
@Table(name = "settlement")
class Settlement(
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    val targetType: SettlementTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(name = "period_start", nullable = false)
    val periodStart: Instant,

    @Column(name = "period_end", nullable = false)
    val periodEnd: Instant,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SettlementStatus = SettlementStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    init {
        require(periodStart.isBefore(periodEnd)) { "정산 기간의 시작은 종료보다 앞서야 합니다." }
    }

    fun transitionTo(next: SettlementStatus) {
        if (!status.canTransitionTo(next)) {
            throw BusinessException(SettlementErrorCode.INVALID_SETTLEMENT_STATUS_TRANSITION)
        }
        status = next
        updatedAt = Instant.now()
    }

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            targetType: SettlementTargetType,
            targetId: Long,
            periodStart: Instant,
            periodEnd: Instant,
            totalAmount: Long = 0,
            status: SettlementStatus = SettlementStatus.PENDING,
        ): Settlement =
            Settlement(targetType, targetId, periodStart, periodEnd, totalAmount, status).also { it.id = id }
    }
}
