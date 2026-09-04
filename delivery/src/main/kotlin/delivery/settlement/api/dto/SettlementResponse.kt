package delivery.settlement.api.dto

import delivery.settlement.application.dto.SettlementItemResult
import delivery.settlement.application.dto.SettlementSummaryResult
import java.math.BigDecimal

data class SettlementResponse(
    val settlementId: Long,
    val targetType: String,
    val targetId: Long,
    val periodStart: String,
    val periodEnd: String,
    val grossAmount: Long,
    val refundAmount: Long,
    val netAmount: Long,
    val feeAmount: Long,
    val payoutAmount: Long,
    val appliedFeeRate: BigDecimal,
    val orderCount: Int,
    val refundCount: Int,
    val status: String,
) {
    companion object {
        fun from(result: SettlementSummaryResult): SettlementResponse =
            SettlementResponse(
                settlementId = result.settlementId,
                targetType = result.targetType.name,
                targetId = result.targetId,
                periodStart = result.periodStart.toString(),
                periodEnd = result.periodEnd.toString(),
                grossAmount = result.grossAmount,
                refundAmount = result.refundAmount,
                netAmount = result.netAmount,
                feeAmount = result.feeAmount,
                payoutAmount = result.payoutAmount,
                appliedFeeRate = result.appliedFeeRate,
                orderCount = result.orderCount,
                refundCount = result.refundCount,
                status = result.status.name,
            )
    }
}

data class SettlementItemResponse(
    val orderId: Long,
    val type: String,
    val amount: Long,
    val appliedFeeRate: BigDecimal,
    val settlementAmount: Long,
) {
    companion object {
        fun from(result: SettlementItemResult): SettlementItemResponse =
            SettlementItemResponse(result.orderId, result.type.name, result.amount, result.appliedFeeRate, result.settlementAmount)
    }
}

data class SettlementItemListResponse(
    val items: List<SettlementItemResponse>,
)

data class AdminSettlementListResponse(
    val settlements: List<SettlementResponse>,
)
