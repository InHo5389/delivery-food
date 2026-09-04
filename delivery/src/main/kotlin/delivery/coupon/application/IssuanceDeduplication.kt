package delivery.coupon.application

import delivery.common.exception.BusinessException
import delivery.coupon.domain.CouponErrorCode
import delivery.coupon.domain.Issuance
import delivery.coupon.infrastructure.IssuanceRepository
import org.springframework.dao.DataIntegrityViolationException

// 같은 사용자가 같은 쿠폰을 동시에 두 번 발급받아도 하나만 성공하도록, "조회 후 없으면
// 생성"이 아니라 유니크 제약(user_id, coupon_id, V23 마이그레이션 참조)이 실제 중복
// 방지를 보장하게 한다(정산 모듈의 saveOrThrowDuplicate와 같은 이유 — DB가 이미
// 보장하는 것을 애플리케이션에서 조회로 다시 하지 않는다).
// Issuance.id가 GenerationType.IDENTITY라 save()가 INSERT를 즉시 실행하므로,
// 위반은 여기서 바로 터진다(플러시가 트랜잭션 끝까지 미뤄지지 않는다).
fun IssuanceRepository.saveOrThrowDuplicate(issuance: Issuance): Issuance =
    try {
        save(issuance)
    } catch (e: DataIntegrityViolationException) {
        throw BusinessException(CouponErrorCode.ALREADY_ISSUED)
    }
