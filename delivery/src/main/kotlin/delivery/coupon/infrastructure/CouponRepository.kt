package delivery.coupon.infrastructure

import delivery.coupon.domain.Coupon
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface CouponRepository : JpaRepository<Coupon, Long> {
    // 매진 검증(조회) ~ 발급수량 증가 ~ 발급 저장을 하나의 트랜잭션으로 직렬화하기 위한
    // SELECT ... FOR UPDATE. 동시 요청이 같은 쿠폰 행을 두고 경합할 때, 뒤 트랜잭션은
    // 앞 트랜잭션이 커밋할 때까지 대기했다가 갱신된 issuedQuantity를 보고 판단한다
    // (커밋 53-9 PAAR — DB 비관적 락).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    fun findByIdForUpdate(id: Long): Coupon?
}
