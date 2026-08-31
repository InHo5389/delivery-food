package delivery.search.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ShopKeywordSearchRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // ⚠️ 의도적 구식 구현 — Phase 3 A-2에서 Elasticsearch + Nori로 개선 예정.
    //   선행 와일드카드(LIKE '%keyword%')는 인덱스를 타지 못해 풀스캔이 발생한다.
    fun searchByKeyword(keyword: String, limit: Int, offset: Int): List<ShopKeywordSearchRow> =
        jdbcTemplate.query(
            """
            SELECT id, name, address, min_order_amount, delivery_fee
            FROM shop
            WHERE status = 'OPEN'
              AND name LIKE CONCAT('%', ?, '%')
            ORDER BY id
            LIMIT ? OFFSET ?
            """.trimIndent(),
            { rs, _ ->
                ShopKeywordSearchRow(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    address = rs.getString("address"),
                    minOrderAmount = rs.getLong("min_order_amount"),
                    deliveryFee = rs.getLong("delivery_fee"),
                )
            },
            keyword, limit, offset,
        )
}
