package delivery.coupon.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.coupon.application.dto.CreateCouponCommand
import delivery.coupon.domain.Coupon
import delivery.coupon.domain.CouponErrorCode
import delivery.coupon.domain.Issuance
import delivery.coupon.infrastructure.CouponRepository
import delivery.coupon.infrastructure.IssuanceRepository
import delivery.shop.application.ShopService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// ★ 모듈 간 호출은 ShopService를 직접 주입해서 쓴다(Facade를 두지 않음, 01_설계원칙.md 4절).
@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val issuanceRepository: IssuanceRepository,
    private val shopService: ShopService,
) {
    // ADMIN은 shopId 없이(플랫폼 전체) 또는 임의 상점으로 생성할 수 있고, OWNER는 본인
    // 상점 전용 쿠폰만 생성할 수 있다(shopId 필수 + 소유권 검증).
    fun createCoupon(requester: AuthenticatedUser, command: CreateCouponCommand): Coupon {
        when (requester.role) {
            Role.ADMIN -> Unit
            Role.OWNER -> {
                val shopId = command.shopId ?: throw BusinessException(CouponErrorCode.SHOP_ID_REQUIRED)
                if (shopService.getById(shopId).ownerId != requester.userId) {
                    throw BusinessException(CouponErrorCode.NOT_OWNER)
                }
            }
            else -> throw BusinessException(CouponErrorCode.NOT_OWNER)
        }
        return couponRepository.save(
            Coupon(
                name = command.name,
                shopId = command.shopId,
                totalQuantity = command.totalQuantity,
                validityDays = command.validityDays,
                startsAt = command.startsAt,
            )
        )
    }

    // ⚠️ 의도적 구식 구현 — MVP는 DB 비관적 락까지만(Phase 3 6-4-3절에서 Redis 원자연산으로 개선 예정).
    // 매진 검증(1) ~ 발급수량 증가(2) ~ 발급 저장(3)이 findByIdForUpdate가 쥔 쿠폰 행 락
    // 안에서 하나의 트랜잭션으로 직렬화된다(커밋 53-9 PAAR).
    @Transactional
    fun issue(couponId: Long, userId: Long): Issuance {
        val coupon = couponRepository.findByIdForUpdate(couponId)
            ?: throw BusinessException(CouponErrorCode.COUPON_NOT_FOUND)

        val startsAt = coupon.startsAt
        if (startsAt != null && startsAt.isAfter(Instant.now())) {
            throw BusinessException(CouponErrorCode.NOT_STARTED)
        }
        if (coupon.issuedQuantity >= coupon.totalQuantity) {
            throw BusinessException(CouponErrorCode.SOLD_OUT)
        }

        coupon.increaseIssuedQuantity()
        // user_id+coupon_id 유니크 제약이 동시 중복 요청의 최종 안전망(커밋 53-7).
        return issuanceRepository.saveOrThrowDuplicate(Issuance(userId, couponId, validityDays = coupon.validityDays))
    }

    fun getMyIssuances(userId: Long): List<Issuance> = issuanceRepository.findAllByUserId(userId)
}
