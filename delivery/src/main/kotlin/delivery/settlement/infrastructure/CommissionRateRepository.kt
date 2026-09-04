package delivery.settlement.infrastructure

import delivery.settlement.domain.CommissionRate
import delivery.settlement.domain.RateType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface CommissionRateRepository : JpaRepository<CommissionRate, Long> {
    // 특정 시점(at) 기준으로 유효했던 가장 최근 요율 하나를 찾는다 — 요율 이력 조회의 핵심 쿼리.
    fun findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
        rateType: RateType,
        at: Instant,
    ): CommissionRate?
}
