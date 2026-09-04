package delivery.coupon.domain

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
import java.time.temporal.ChronoUnit

// userId, couponId는 각각 auth 모듈, 같은 coupon 모듈 내 Coupon에 대한 참조다.
// couponId는 같은 모듈 내 참조라 FK를 허용하지만(모듈 간 FK만 금지), 발급 시점의
// validityDays를 expiresAt으로 스냅샷해두므로 조회 시 Coupon을 다시 조인할 필요는 없다
// (나중에 쿠폰의 validityDays가 바뀌어도 이미 발급된 건의 만료일은 그대로 유지되어야 한다).
@Entity
@Table(name = "issuance")
class Issuance(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: Instant = Instant.now(),

    validityDays: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: IssuanceStatus = IssuanceStatus.ISSUED
        protected set

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant = issuedAt.plus(validityDays.toLong(), ChronoUnit.DAYS)

    @Column(name = "used_at")
    var usedAt: Instant? = null
        protected set

    fun use() {
        transitionTo(IssuanceStatus.USED)
        usedAt = Instant.now()
    }

    fun expire() {
        transitionTo(IssuanceStatus.EXPIRED)
    }

    private fun transitionTo(next: IssuanceStatus) {
        if (!status.canTransitionTo(next)) {
            throw BusinessException(CouponErrorCode.INVALID_ISSUANCE_STATUS_TRANSITION)
        }
        status = next
    }

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            userId: Long,
            couponId: Long,
            issuedAt: Instant,
            validityDays: Int,
            status: IssuanceStatus = IssuanceStatus.ISSUED,
            usedAt: Instant? = null,
        ): Issuance =
            Issuance(userId, couponId, issuedAt, validityDays).also {
                it.id = id
                it.status = status
                it.usedAt = usedAt
            }
    }
}
