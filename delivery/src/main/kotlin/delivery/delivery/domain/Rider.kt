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
        updatedAt = Instant.now()
    }

    fun goBusy() {
        status = RiderStatus.BUSY
        updatedAt = Instant.now()
    }

    fun goOffline() {
        status = RiderStatus.OFFLINE
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
        ): Rider = Rider(
            accountId = accountId,
            latitude = latitude,
            longitude = longitude,
            status = status,
        ).also { it.id = id }
    }
}
