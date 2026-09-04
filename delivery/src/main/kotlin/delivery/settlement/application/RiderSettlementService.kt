package delivery.settlement.application

import delivery.delivery.application.DeliveryService
import delivery.order.application.OrderService
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementItem
import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.SettlementItemRepository
import delivery.settlement.infrastructure.SettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth

// 배달비는 라이더 몫이라 요율(플랫폼 수수료) 적용 대상이 아니다(01_설계원칙.md, 04_Phase1
// 커밋 28-29 결정 사항). 그래서 상점 정산과 달리 CommissionRate를 조회하지 않는다.
private val NO_FEE_RATE: BigDecimal = BigDecimal("0.0000")

// 배달을 이미 완료했다면 음식 환불과 무관하게 배달비를 받아야 하므로(라이더 귀책이 아님),
// DELIVERED 건만 대상으로 하고 환불 차감/이월 로직은 아예 없다 — 상점 정산보다 단순하다.
// ★ 모듈 간 호출은 DeliveryService/OrderService를 직접 주입해서 쓴다(Facade를 두지 않음,
//   01_설계원칙.md 4절 — 모놀리스 단계에서는 직접 호출이 허용된다).
@Service
class RiderSettlementService(
    private val settlementRepository: SettlementRepository,
    private val settlementItemRepository: SettlementItemRepository,
    private val deliveryService: DeliveryService,
    private val orderService: OrderService,
) {
    // 같은 기간 중복 계산 방지는 사전 조회가 아니라 저장 시점의 유니크 제약으로
    // 보장한다(SettlementDeduplication.kt 참조, ShopSettlementService와 동일한 이유).
    @Transactional
    fun calculateRiderSettlement(riderId: Long, yearMonth: YearMonth): Settlement {
        val (start, end) = monthRange(yearMonth)

        val orderIds = deliveryService.getDeliveredOrderIds(riderId, start, end)
        val deliveryFees = orderService.getDeliveryFees(orderIds)
        val totalAmount = orderIds.sumOf { deliveryFees[it] ?: 0L }

        val settlement = settlementRepository.saveOrThrowDuplicate(
            Settlement(
                targetType = SettlementTargetType.RIDER,
                targetId = riderId,
                periodStart = start,
                periodEnd = end,
                totalAmount = totalAmount,
            )
        )

        val items = orderIds.map { orderId ->
            val fee = deliveryFees[orderId] ?: 0L
            SettlementItem(
                settlementId = settlement.id!!,
                orderId = orderId,
                type = SettlementItemType.SALE,
                amount = fee,
                appliedFeeRate = NO_FEE_RATE,
                settlementAmount = fee,
            )
        }
        settlementItemRepository.saveAll(items)

        return settlement
    }
}
