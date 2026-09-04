package delivery.settlement.infrastructure

import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementTargetType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface SettlementRepository : JpaRepository<Settlement, Long> {
    // 같은 대상·같은 기간의 정산이 이미 있는지 확인하는 중복 생성 방지 조회.
    fun findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(
        targetType: SettlementTargetType,
        targetId: Long,
        periodStart: Instant,
        periodEnd: Instant,
    ): Settlement?

    // 사장님/라이더가 "내 정산 내역"을 기간 최신순으로 조회하는 화면에서 쓴다.
    fun findAllByTargetTypeAndTargetIdOrderByPeriodStartDesc(
        targetType: SettlementTargetType,
        targetId: Long,
    ): List<Settlement>
}
