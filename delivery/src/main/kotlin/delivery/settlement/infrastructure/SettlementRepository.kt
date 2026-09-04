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

    // 운영자 전체 정산 목록 조회(GET /admin/settlements?from=&to=)에서 쓴다. 전체 대상을
    // 아우르는 기간 범위 조회라 target_type/target_id로 좁힐 수 없어 인덱스 효과가 없다 —
    // ⚠️ 의도적 구식 구현(운영자 전용 저빈도 화면이라 지금은 전체 스캔을 허용, Phase 3에서
    // 필요해지면 그때 인덱스/집계 테이블을 검토한다).
    fun findAllByPeriodStartGreaterThanEqualAndPeriodStartLessThanOrderByPeriodStartDesc(
        from: Instant,
        to: Instant,
    ): List<Settlement>
}
