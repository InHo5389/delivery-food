package delivery.settlement.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

// settlement_id는 같은 settlement 모듈 내 참조라 FK를 허용한다(모듈 간 FK만 금지).
// order_id는 다른 모듈(order) 테이블에 대한 논리 참조라 FK를 걸지 않는다.
//
// appliedFeeRate는 이 항목을 계산할 당시 적용된 요율의 스냅샷이다(CommissionRate 이력의
// 특정 시점 값을 복사). 나중에 요율이 바뀌어도, 심지어 같은 주문이 몇 달 뒤 다른 정산
// 회차에서 환불로 다시 등장해도(그때는 새 SettlementItem row가 추가됨) 원래 계산에 쓰인
// 요율이 그대로 보존된다 — "왜 이 금액인가"를 항상 그때 기준으로 재현하기 위한 근거다.
// amount는 그 주문의 판매/환불 원금(항상 양수), settlementAmount는 요율을 반영해 실제
// 정산에 기여하는 부호 있는 금액이다(SALE이면 +, REFUND면 -).
@Entity
@Table(name = "settlement_item")
class SettlementItem(
    @Column(name = "settlement_id", nullable = false)
    val settlementId: Long,

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: SettlementItemType,

    @Column(nullable = false)
    val amount: Long,

    @Column(name = "applied_fee_rate", nullable = false, precision = 5, scale = 4)
    val appliedFeeRate: BigDecimal,

    @Column(name = "settlement_amount", nullable = false)
    val settlementAmount: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun withId(
            id: Long,
            settlementId: Long,
            orderId: Long,
            type: SettlementItemType,
            amount: Long,
            appliedFeeRate: BigDecimal,
            settlementAmount: Long,
        ): SettlementItem =
            SettlementItem(settlementId, orderId, type, amount, appliedFeeRate, settlementAmount).also { it.id = id }
    }
}
