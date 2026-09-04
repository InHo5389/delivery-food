package delivery.delivery.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// delivery_id/rider_id는 같은 delivery 모듈 내 참조라 FK를 허용한다(설계 원칙 5절 —
// 모듈 간 FK만 금지). 반경 내 AVAILABLE 라이더 전원에게 동시에 오퍼를 보내고 먼저
// 수락하는 사람이 배정되는 선착순 방식이라, 라이더 간 순위를 매길 이유가 없다.
@Entity
@Table(name = "dispatch_offer")
class DispatchOffer(
    @Column(name = "delivery_id", nullable = false)
    val deliveryId: Long,

    @Column(name = "rider_id", nullable = false)
    val riderId: Long,

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
            status: DispatchOfferStatus = DispatchOfferStatus.SENT,
        ): DispatchOffer = DispatchOffer(deliveryId, riderId, status).also { it.id = id }
    }
}
