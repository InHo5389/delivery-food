package delivery.coupon.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// shopId는 shop 모듈 테이블에 대한 논리 참조라 FK를 걸지 않는다(모듈 간 FK 금지).
// null이면 특정 상점이 아니라 플랫폼 전체에 적용되는 쿠폰이다.
//
// issuedQuantity 증가와 매진(totalQuantity 초과) 검증은 이 엔티티가 아니라 서비스 계층의
// 책임이다(커밋 53-9) — 두 단계가 "쿠폰 행을 잠근 트랜잭션 안"이라는 락 경계에 함께 있어야
// 하는 동시성 제어라, 엔티티 메서드 하나로 캡슐화하면 그 경계가 드러나지 않는다.
@Entity
@Table(name = "coupon")
class Coupon(
    @Column(nullable = false)
    val name: String,

    @Column(name = "shop_id")
    val shopId: Long? = null,

    @Column(name = "total_quantity", nullable = false)
    val totalQuantity: Int,

    @Column(name = "issued_quantity", nullable = false)
    var issuedQuantity: Int = 0,

    @Column(name = "validity_days", nullable = false)
    val validityDays: Int,

    @Column(name = "starts_at")
    val startsAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    init {
        require(totalQuantity > 0) { "쿠폰 수량은 1개 이상이어야 합니다." }
        require(validityDays > 0) { "쿠폰 유효기간은 1일 이상이어야 합니다." }
    }

    fun increaseIssuedQuantity() {
        issuedQuantity++
    }

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            name: String,
            shopId: Long? = null,
            totalQuantity: Int,
            issuedQuantity: Int = 0,
            validityDays: Int,
            startsAt: Instant? = null,
        ): Coupon =
            Coupon(name, shopId, totalQuantity, issuedQuantity, validityDays, startsAt).also { it.id = id }
    }
}
