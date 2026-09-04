package delivery.settlement.application.dto

import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementStatus
import delivery.settlement.domain.SettlementTargetType
import java.math.BigDecimal
import java.time.YearMonth

data class SettlementSummaryResult(
    val settlementId: Long,
    val targetType: SettlementTargetType,
    val targetId: Long,
    val period: YearMonth,
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
