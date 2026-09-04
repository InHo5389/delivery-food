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

// 수수료율은 시간에 따라 바뀌는 정책값이라 이력으로 저장한다 — Menu.price와 같은 이유다.
// "지금 요율이 몇 %인가"가 아니라 "특정 시점(effectiveFrom 이하 중 가장 최근)의 요율이
// 몇 %였는가"를 물어야 과거 정산을 그때 요율로 재현할 수 있다. effectiveTo 컬럼을 따로
// 두지 않는 이유: 새 요율을 추가하는 순간 이전 구간의 끝은 자동으로 정해지므로(다음
// 유효 시작 시각 = 이전 요율의 끝), 별도 컬럼을 두면 두 값이 어긋날 여지만 생긴다.
@Entity
@Table(name = "commission_rate")
class CommissionRate(
    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false)
    val rateType: RateType,

    @Column(nullable = false, precision = 5, scale = 4)
    val rate: BigDecimal,

    @Column(name = "effective_from", nullable = false)
    val effectiveFrom: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(id: Long, rateType: RateType, rate: BigDecimal, effectiveFrom: Instant): CommissionRate =
            CommissionRate(rateType, rate, effectiveFrom).also { it.id = id }
    }
}
