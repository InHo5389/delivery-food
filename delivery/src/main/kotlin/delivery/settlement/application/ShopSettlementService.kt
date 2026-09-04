package delivery.settlement.application

import delivery.common.exception.BusinessException
import delivery.order.application.OrderService
import delivery.order.application.dto.ShopSettlementSourceItem
import delivery.settlement.domain.RateType
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementErrorCode
import delivery.settlement.domain.SettlementItem
import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.CommissionRateRepository
import delivery.settlement.infrastructure.SettlementItemRepository
import delivery.settlement.infrastructure.SettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

// ★ 모듈 간 호출은 OrderService를 직접 주입해서 쓴다(Facade를 두지 않음, 01_설계원칙.md
//   4절 — 모놀리스 단계에서는 직접 호출이 허용된다).
@Service
class ShopSettlementService(
    private val settlementRepository: SettlementRepository,
    private val settlementItemRepository: SettlementItemRepository,
    private val commissionRateRepository: CommissionRateRepository,
    private val orderService: OrderService,
) {
    // 상점 정산은 주(월~일) 단위다(53-6 — 매주 월요일 새벽 3시, 지난주 집계). 취소 주에
    // 음수 항목으로 반영하고, 정산액이 음수면 다음 주로 이월한다(원주문 주를 소급
    // 수정하지 않는다 — 이미 확정·지급된 정산을 되돌리지 않기 위함).
    // 같은 기간 중복 계산 방지는 사전 조회가 아니라 저장 시점의 유니크 제약으로
    // 보장한다(SettlementDeduplication.kt 참조).
    @Transactional
    fun calculateShopSettlement(shopId: Long, weekStart: LocalDate): Settlement {
        val (start, end) = weekRange(weekStart)

        val rate = commissionRateRepository
            .findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(RateType.PLATFORM_FEE, start)
            ?.rate
            ?: throw BusinessException(SettlementErrorCode.COMMISSION_RATE_NOT_FOUND)

        val sales = orderService.getDeliveredOrderAmounts(shopId, start, end)
        val refunds = orderService.getRefundedPaymentAmounts(shopId, start, end)

        val saleAmounts = sales.map { it.orderId to settleAmount(it.amount, rate) }
        val refundAmounts = refunds.map { it.orderId to -settleAmount(it.amount, appliedRefundRate(it, rate)) }

        val carriedOver = previousCarryOver(shopId, weekStart)
        val totalAmount = saleAmounts.sumOf { it.second } + refundAmounts.sumOf { it.second } + carriedOver

        val settlement = settlementRepository.saveOrThrowDuplicate(
            Settlement(
                targetType = SettlementTargetType.SHOP,
                targetId = shopId,
                periodStart = start,
                periodEnd = end,
                totalAmount = totalAmount,
                carriedOverAmount = carriedOver,
            )
        )

        val items = sales.zip(saleAmounts).map { (source, settled) ->
            newItem(settlement.id!!, source, SettlementItemType.SALE, rate, settled.second)
        } + refunds.zip(refundAmounts).map { (source, settled) ->
            newItem(settlement.id!!, source, SettlementItemType.REFUND, appliedRefundRate(source, rate), settled.second)
        }
        settlementItemRepository.saveAll(items)

        return settlement
    }

    // 판매 원금에서 요율만큼 뗀 나머지가 상점에 돌아가는 금액이다. 원 단위 반올림은
    // 소비자에게 유리하지도 불리하지도 않은 반올림(HALF_UP)을 쓴다.
    private fun settleAmount(amount: Long, rate: BigDecimal): Long {
        val fee = BigDecimal(amount).multiply(rate).setScale(0, RoundingMode.HALF_UP).toLong()
        return amount - fee
    }

    // 환불 건은 원래 그 주문이 판매로 잡혔을 때 적용됐던 요율을 그대로 재사용한다 —
    // 이번 달 요율이 바뀌었어도 "그때 지급한 만큼만 정확히 회수"해야 하기 때문이다.
    // 지금 OrderStatus 상태머신상 DELIVERED(판매 집계 대상) 이후에는 취소/환불 경로가
    // 없어 이 조회가 맞는 이력을 찾지 못하는 게 현재는 일반적인 경우다 — 그런 경우
    // 이번 정산 기간의 요율로 대체한다(향후 배달완료 후 환불 기능이 생기면 그때부터
    // 이 조회가 실제 이력을 찾아 쓰게 된다).
    private fun appliedRefundRate(source: ShopSettlementSourceItem, currentRate: BigDecimal): BigDecimal =
        settlementItemRepository
            .findTopByOrderIdAndTypeOrderByCreatedAtDesc(source.orderId, SettlementItemType.SALE)
            ?.appliedFeeRate
            ?: currentRate

    private fun newItem(
        settlementId: Long,
        source: ShopSettlementSourceItem,
        type: SettlementItemType,
        appliedRate: BigDecimal,
        settlementAmount: Long,
    ): SettlementItem = SettlementItem(
        settlementId = settlementId,
        orderId = source.orderId,
        type = type,
        amount = source.amount,
        appliedFeeRate = appliedRate,
        settlementAmount = settlementAmount,
    )

    // 직전 주 정산이 이미 존재하고 그 결과(totalAmount)가 음수였다면 이번 주로 그대로 이월한다.
    // 직전 정산 기록 자체는 확정된 이력이라 건드리지 않는다.
    private fun previousCarryOver(shopId: Long, weekStart: LocalDate): Long {
        val (previousStart, previousEnd) = weekRange(weekStart.minusWeeks(1))
        val previous = settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(
            SettlementTargetType.SHOP, shopId, previousStart, previousEnd,
        ) ?: return 0L
        return if (previous.totalAmount < 0) previous.totalAmount else 0L
    }
}
