package delivery.order.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

// SalesSummaryRepository와 같은 orders/order_items/payment 테이블을 보되, 정산은 상점 매출
// "합계"가 아니라 주문 하나하나에 요율을 적용해 SettlementItem으로 남겨야 해서(53-1 참조)
// 주문 단위 행을 그대로 돌려준다. ⚠️ 의도적 구식 구현 — SalesSummaryRepository와 동일하게
// 조회할 때마다 실시간 집계한다(Phase 3에서 배치/집계테이블로 개선 예정, F-3/F-4 참조).
@Repository
class ShopSettlementSourceRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findDeliveredOrderAmounts(shopId: Long, from: Instant, to: Instant): List<ShopSettlementSourceRow> =
        jdbcTemplate.query(
            """
            SELECT o.id AS order_id, SUM(oi.menu_price * oi.quantity) AS amount
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.shop_id = ? AND o.status = 'DELIVERED' AND o.updated_at >= ? AND o.updated_at < ?
            GROUP BY o.id
            """.trimIndent(),
            { rs, _ -> ShopSettlementSourceRow(orderId = rs.getLong("order_id"), amount = rs.getLong("amount")) },
            shopId, Timestamp.from(from), Timestamp.from(to),
        )

    // 환불 대상은 Order.status가 아니라 Payment.status = 'REFUNDED'로 판정한다 — Order만
    // 보면 "결제 전(CREATED)에 취소돼 애초에 환불 자체가 없었던 건"까지 섞여 실제로 돈이
    // 나가지 않은 취소까지 환불로 잘못 집계될 수 있다.
    fun findRefundedPaymentAmounts(shopId: Long, from: Instant, to: Instant): List<ShopSettlementSourceRow> =
        jdbcTemplate.query(
            """
            SELECT o.id AS order_id, p.amount AS amount
            FROM payment p
            JOIN orders o ON o.id = p.order_id
            WHERE o.shop_id = ? AND p.status = 'REFUNDED' AND p.updated_at >= ? AND p.updated_at < ?
            """.trimIndent(),
            { rs, _ -> ShopSettlementSourceRow(orderId = rs.getLong("order_id"), amount = rs.getLong("amount")) },
            shopId, Timestamp.from(from), Timestamp.from(to),
        )
}

data class ShopSettlementSourceRow(val orderId: Long, val amount: Long)
