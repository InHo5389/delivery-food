package delivery.settlement.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.delivery.application.DeliveryService
import delivery.settlement.application.dto.AdminSettlementRangeQuery
import delivery.settlement.application.dto.MySettlementQuery
import delivery.settlement.application.dto.SettlementItemResult
import delivery.settlement.application.dto.SettlementSummaryResult
import delivery.settlement.domain.RateType
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementErrorCode
import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementStatus
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.CommissionRateRepository
import delivery.settlement.infrastructure.SettlementItemRepository
import delivery.settlement.infrastructure.SettlementRepository
import delivery.shop.application.ShopService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

// ★ 모듈 간 호출은 ShopService/DeliveryService를 직접 주입해서 쓴다(Facade를 두지 않음,
//   01_설계원칙.md 4절 — 모놀리스 단계에서는 직접 호출이 허용된다).
@Service
class SettlementQueryService(
    private val settlementRepository: SettlementRepository,
    private val settlementItemRepository: SettlementItemRepository,
    private val commissionRateRepository: CommissionRateRepository,
    private val shopService: ShopService,
    private val deliveryService: DeliveryService,
) {
    // 권한 검증: JWT의 userId로 본인 소유 대상(상점/라이더)만 조회한다 — 남의 정산은 볼 수 없다.
    // 형식 검증(날짜 파싱)을 소유권 조회보다 먼저 해서, 요청 자체가 잘못됐을 땐 굳이
    // 상점/라이더 조회까지 가지 않고 400을 먼저 돌려준다.
    //
    // date 하나로 라이더(하루)/상점(그 주 월~일)을 모두 받는다 — 호출하는 쪽이 "이 날짜가
    // 속한 정산을 보고 싶다"고만 넘기면, 상점 정산일 때는 그 주의 월요일로 자동 보정한다
    // (사장님이 정산 기간이 항상 월요일부터라는 걸 미리 알아야 할 필요가 없게).
    fun getMySettlement(requester: AuthenticatedUser, query: MySettlementQuery): SettlementSummaryResult {
        val date = parseDate(query.date)
        val (targetType, targetId) = resolveOwnTarget(requester, query.shopId)
        val (start, end) = when (targetType) {
            SettlementTargetType.SHOP -> weekRange(mondayOf(date))
            SettlementTargetType.RIDER -> dayRange(date)
        }
        val settlement = settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(targetType, targetId, start, end)
            ?: throw BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND)
        return toSummary(settlement)
    }

    fun getSettlementItems(requester: AuthenticatedUser, settlementId: Long): List<SettlementItemResult> {
        val settlement = getSettlementOrThrow(settlementId)
        assertCanView(requester, settlement)
        return settlementItemRepository.findAllBySettlementId(settlementId)
            .map { SettlementItemResult(it.orderId, it.type, it.amount, it.appliedFeeRate, it.settlementAmount) }
    }

    // ⚠️ 의도적 구식 구현 — SettlementRepository 주석 참조(전체 스캔, N+1 없음이지만 대상별
    //   페이징도 없음). 운영자 전용 저빈도 화면이라 Phase 3 이전까지는 이 정도로 충분하다.
    // from/to는 날짜 범위이고, 라이더(일 단위)·상점(주 단위) 정산이 같은 테이블에 섞여
    // 있어 periodStart가 이 범위 안에 있는 모든 정산을 기간 종류와 무관하게 반환한다.
    fun getAdminSettlements(requester: AuthenticatedUser, query: AdminSettlementRangeQuery): List<SettlementSummaryResult> {
        assertAdmin(requester)
        val start = dayRange(parseDate(query.from)).first
        val end = dayRange(parseDate(query.to)).second
        return settlementRepository.findAllByPeriodStartGreaterThanEqualAndPeriodStartLessThanOrderByPeriodStartDesc(start, end)
            .map { toSummary(it) }
    }

    @Transactional
    fun confirmSettlement(requester: AuthenticatedUser, settlementId: Long): SettlementSummaryResult {
        assertAdmin(requester)
        val settlement = getSettlementOrThrow(settlementId)
        settlement.transitionTo(SettlementStatus.CONFIRMED)
        return toSummary(settlement)
    }

    @Transactional
    fun paySettlement(requester: AuthenticatedUser, settlementId: Long): SettlementSummaryResult {
        assertAdmin(requester)
        val settlement = getSettlementOrThrow(settlementId)
        settlement.transitionTo(SettlementStatus.PAID)
        return toSummary(settlement)
    }

    private fun resolveOwnTarget(requester: AuthenticatedUser, shopId: Long?): Pair<SettlementTargetType, Long> =
        when (requester.role) {
            Role.OWNER -> {
                val id = shopId ?: throw BusinessException(SettlementErrorCode.SHOP_ID_REQUIRED)
                val shop = shopService.getById(id)
                if (shop.ownerId != requester.userId) {
                    throw BusinessException(SettlementErrorCode.NOT_SETTLEMENT_OWNER)
                }
                SettlementTargetType.SHOP to id
            }
            Role.RIDER -> SettlementTargetType.RIDER to deliveryService.getRiderIdByAccountId(requester.userId)
            else -> throw BusinessException(SettlementErrorCode.NOT_SETTLEMENT_OWNER)
        }

    private fun assertCanView(requester: AuthenticatedUser, settlement: Settlement) {
        if (requester.role == Role.ADMIN) return
        val owned = when (settlement.targetType) {
            SettlementTargetType.SHOP ->
                requester.role == Role.OWNER && shopService.getById(settlement.targetId).ownerId == requester.userId
            SettlementTargetType.RIDER ->
                requester.role == Role.RIDER && deliveryService.getRiderAccountId(settlement.targetId) == requester.userId
        }
        if (!owned) {
            throw BusinessException(SettlementErrorCode.NOT_SETTLEMENT_OWNER)
        }
    }

    private fun assertAdmin(requester: AuthenticatedUser) {
        if (requester.role != Role.ADMIN) {
            throw BusinessException(SettlementErrorCode.NOT_ADMIN)
        }
    }

    private fun getSettlementOrThrow(settlementId: Long): Settlement =
        settlementRepository.findById(settlementId).orElseThrow { BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND) }

    // 상세 항목(SettlementItem)에서 응답에 필요한 요약 수치를 다시 계산한다 — Settlement에
    // grossAmount 등을 별도 컬럼으로 미리 저장해두지 않는다. 항목 목록이 이미 근거로
    // 저장돼 있어(53-1) 조회 시점에 합산하는 쪽이 컬럼 중복을 만들지 않는다.
    private fun toSummary(settlement: Settlement): SettlementSummaryResult {
        val items = settlementItemRepository.findAllBySettlementId(settlement.id!!)
        val sales = items.filter { it.type == SettlementItemType.SALE }
        val refunds = items.filter { it.type == SettlementItemType.REFUND }
        val grossAmount = sales.sumOf { it.amount }
        val refundAmount = refunds.sumOf { it.amount }
        val netAmount = grossAmount - refundAmount
        val payoutAmount = settlement.totalAmount
        // 요율은 항목마다 다를 수 있어(과거 요율 재사용 케이스, ShopSettlementService 참조)
        // 그 합을 역산한 값이지, 단일 요율을 적용해 다시 계산한 값이 아니다.
        val feeAmount = netAmount - (payoutAmount - settlement.carriedOverAmount)
        return SettlementSummaryResult(
            settlementId = settlement.id!!,
            targetType = settlement.targetType,
            targetId = settlement.targetId,
            periodStart = toKstDate(settlement.periodStart),
            periodEnd = toKstDate(settlement.periodEnd),
            grossAmount = grossAmount,
            refundAmount = refundAmount,
            netAmount = netAmount,
            feeAmount = feeAmount,
            payoutAmount = payoutAmount,
            appliedFeeRate = currentDisplayRate(settlement),
            orderCount = sales.size,
            refundCount = refunds.size,
            status = settlement.status,
        )
    }

    private fun toKstDate(instant: Instant): LocalDate = LocalDate.ofInstant(instant, KST)

    // 응답에 참고용으로 보여주는 대표 요율이다(실제 계산은 이미 항목별로 끝나 있어
    // 이 값이 금액에 영향을 주지 않는다). 라이더 정산은 요율 개념이 없어 0으로 고정.
    private fun currentDisplayRate(settlement: Settlement): BigDecimal =
        when (settlement.targetType) {
            SettlementTargetType.SHOP ->
                commissionRateRepository
                    .findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(RateType.PLATFORM_FEE, settlement.periodStart)
                    ?.rate
                    ?: BigDecimal.ZERO
            SettlementTargetType.RIDER -> BigDecimal.ZERO
        }

    private fun parseDate(value: String): LocalDate =
        try {
            LocalDate.parse(value)
        } catch (e: DateTimeParseException) {
            throw BusinessException(SettlementErrorCode.INVALID_DATE)
        }
}
