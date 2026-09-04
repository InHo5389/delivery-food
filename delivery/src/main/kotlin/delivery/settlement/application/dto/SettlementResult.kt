package delivery.settlement.application.dto

import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementStatus
import delivery.settlement.domain.SettlementTargetType
import java.math.BigDecimal
import java.time.LocalDate

data class SettlementSummaryResult(
    val settlementId: Long,
    val targetType: SettlementTargetType,
    val targetId: Long,
    // periodEnd는 반열림 구간의 끝(그 날짜는 포함하지 않음)이다 — 라이더는 하루, 상점은
    // 월요일부터 시작하는 7일이라 periodStart/periodEnd 둘 다 있어야 기간을 알 수 있다.
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val grossAmount: Long,
    val refundAmount: Long,
    val netAmount: Long,
    val feeAmount: Long,
    val payoutAmount: Long,
    val appliedFeeRate: BigDecimal,
    val orderCount: Int,
    val refundCount: Int,
    val status: SettlementStatus,
)

data class SettlementItemResult(
    val orderId: Long,
    val type: SettlementItemType,
    val amount: Long,
    val appliedFeeRate: BigDecimal,
    val settlementAmount: Long,
)
