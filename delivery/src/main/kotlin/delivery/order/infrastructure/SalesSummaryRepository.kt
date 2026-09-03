package delivery.order.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class SalesSummaryRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // ⚠️ 의도적 구식 구현 — 조회할 때마다 orders/order_items를 실시간으로 GROUP BY한다.
    //   주문이 쌓일수록 이 쿼리 자체가 느려진다. 나중에 일자별 집계 테이블로 미리
    //   계산해두는 방식으로 개선할 여지를 남겨둔다.
    fun findSales(shopId: Long, from: Instant, to: Instant): SalesSummaryRow =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(DISTINCT o.id) AS order_count,
                   COALESCE(SUM(oi.menu_price * oi.quantity), 0) AS total_amount
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.shop_id = ? AND o.status = 'DELIVERED' AND o.updated_at >= ? AND o.updated_at < ?
            """.trimIndent(),
            { rs, _ ->
                SalesSummaryRow(
                    orderCount = rs.getLong("order_count"),
                    totalAmount = rs.getLong("total_amount"),
                )
            },
            shopId, Timestamp.from(from), Timestamp.from(to),
        )!!
}
