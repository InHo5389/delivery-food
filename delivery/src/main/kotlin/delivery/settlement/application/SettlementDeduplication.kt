package delivery.settlement.application

import delivery.common.exception.BusinessException
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementErrorCode
import delivery.settlement.infrastructure.SettlementRepository
import org.springframework.dao.DataIntegrityViolationException

// 같은 대상·같은 기간의 정산이 동시에 두 번 계산돼도 하나만 성공하도록, "조회 후 없으면
// 생성"이 아니라 유니크 제약(target_type, target_id, period_start, period_end, V18
// 마이그레이션 참조)이 실제 중복 방지를 보장하게 한다 — 배차 CAS와 같은 원리로, DB가
// 이미 보장하는 것을 애플리케이션에서 조회로 다시 하지 않는다("조회 후 없으면 생성"은
// 두 요청이 동시에 조회를 통과해버릴 수 있는 race condition이 있다).
// Settlement.id가 GenerationType.IDENTITY라 save()가 INSERT를 즉시 실행하므로, 위반은
// 여기서 바로 터진다(플러시가 트랜잭션 끝까지 미뤄지지 않는다).
fun SettlementRepository.saveOrThrowDuplicate(settlement: Settlement): Settlement =
    try {
        save(settlement)
    } catch (e: DataIntegrityViolationException) {
        throw BusinessException(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS)
    }
