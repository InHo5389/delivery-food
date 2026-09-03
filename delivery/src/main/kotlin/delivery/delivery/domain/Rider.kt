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

// account_id는 다른 모듈(auth) 테이블에 대한 논리 참조다. 모듈 경계 규칙(설계 원칙 5절)에
// 따라 FK를 걸지 않는다.
@Entity
@Table(name = "rider")
class Rider(
    @Column(name = "account_id", nullable = false)
    val accountId: Long,

    @Column(nullable = false)
    var latitude: BigDecimal,

    @Column(nullable = false)
    var longitude: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RiderStatus = RiderStatus.OFFLINE,

    // 최근 처리 건수 — 배차 점수화에서 특정 라이더에게 오퍼가 몰리는 것을 막는 데 쓴다.
    @Column(name = "recent_delivery_count", nullable = false)
    var recentDeliveryCount: Int = 0,

    @Column(name = "acceptance_rate", nullable = false)
    var acceptanceRate: BigDecimal = BigDecimal("1.0000"),

    // AVAILABLE로 전환된 시각. 배차 점수화의 "대기시간" 요소를 계산하는 기준이다.
    @Column(name = "available_since")
    var availableSince: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun goAvailable() {
        status = RiderStatus.AVAILABLE
        availableSince = Instant.now()
        updatedAt = Instant.now()
    }

    fun goBusy() {
        status = RiderStatus.BUSY
        availableSince = null
        updatedAt = Instant.now()
    }

    fun goOffline() {
        status = RiderStatus.OFFLINE
        availableSince = null
        updatedAt = Instant.now()
    }

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            accountId: Long,
            latitude: BigDecimal = BigDecimal("37.5665000"),
            longitude: BigDecimal = BigDecimal("126.9780000"),
            status: RiderStatus = RiderStatus.AVAILABLE,
            recentDeliveryCount: Int = 0,
            acceptanceRate: BigDecimal = BigDecimal("1.0000"),
            availableSince: Instant? = Instant.now(),
        ): Rider = Rider(
            accountId = accountId,
            latitude = latitude,
            longitude = longitude,
            status = status,
            recentDeliveryCount = recentDeliveryCount,
            acceptanceRate = acceptanceRate,
            availableSince = availableSince,
        ).also { it.id = id }
    }
}
