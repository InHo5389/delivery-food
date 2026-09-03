package delivery.delivery.domain

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

// delivery_id/rider_id는 같은 delivery 모듈 내 참조라 FK를 허용한다(설계 원칙 5절 —
// 모듈 간 FK만 금지). score는 오퍼 발송 시점 DispatchScorer의 계산 결과 스냅샷이다 —
// 이후 라이더 지표가 바뀌어도 이 오퍼가 왜 이 순위였는지 그대로 남는다.
@Entity
@Table(name = "dispatch_offer")
class DispatchOffer(
    @Column(name = "delivery_id", nullable = false)
    val deliveryId: Long,

    @Column(name = "rider_id", nullable = false)
    val riderId: Long,

    @Column(nullable = false)
    val score: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DispatchOfferStatus = DispatchOfferStatus.SENT,

    @Column(name = "offered_at", nullable = false)
    val offeredAt: Instant = Instant.now(),

    @Column(name = "responded_at")
    var respondedAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            deliveryId: Long,
            riderId: Long,
            score: BigDecimal = BigDecimal("1.0000"),
            status: DispatchOfferStatus = DispatchOfferStatus.SENT,
        ): DispatchOffer = DispatchOffer(deliveryId, riderId, score, status).also { it.id = id }
    }
}
