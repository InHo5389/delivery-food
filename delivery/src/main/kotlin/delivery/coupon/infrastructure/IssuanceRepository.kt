package delivery.coupon.infrastructure

import delivery.coupon.domain.Issuance
import delivery.coupon.domain.IssuanceStatus
import org.springframework.data.jpa.repository.JpaRepository

interface IssuanceRepository : JpaRepository<Issuance, Long> {
    // 유니크 제약(user_id, coupon_id)이 강제하는 "1인 1매"를 애플리케이션 레벨에서도
    // 먼저 걸러 불필요한 예외를 줄이는 사전 조회(최종 방어선은 여전히 DB 제약, 커밋 53-7 PAAR).
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean

    // 쿠폰 상세에서 발급 목록을 조회하는 화면(운영자)에서 쓴다.
    fun findAllByCouponId(couponId: Long): List<Issuance>

    // 만료 배치가 상태별로 대상(ISSUED이면서 유효기간이 지난 건)을 골라내는 데 쓴다.
    fun findAllByStatus(status: IssuanceStatus): List<Issuance>

    // "내 쿠폰 목록" 조회 화면에서 쓴다.
    fun findAllByUserId(userId: Long): List<Issuance>
}
