package delivery.delivery.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class DispatchQueueRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // 목록을 보여주기만 하는 조회라 잠글 필요가 없다. 잠금은 claimNext()처럼 "보고 바로
    // 가져가는" 조회에만 필요하다.
    fun findQueue(limit: Int): List<DispatchQueueRow> =
        jdbcTemplate.query(
            """
            SELECT id, order_id, shop_id, estimated_pickup_at
            FROM delivery
            WHERE status = 'OFFERING'
            ORDER BY created_at ASC
            LIMIT ?
            """.trimIndent(),
            { rs, _ ->
                DispatchQueueRow(
                    deliveryId = rs.getLong("id"),
                    orderId = rs.getLong("order_id"),
                    shopId = rs.getLong("shop_id"),
                    estimatedPickupAt = rs.getTimestamp("estimated_pickup_at")?.let(Timestamp::toInstant),
                )
            },
            limit,
        )

    // ⚠️ 의도적 구식 구현 — 큐 맨 앞 배달 1건을 SELECT ... FOR UPDATE로 잠근 뒤 그 안에서
    // 배정까지 끝낸다. 라이더 여러 명이 동시에 클레임을 시도하면 전부 같은 행(큐가 짧을 땐
    // 정확히 같은 1행)을 잠그려고 경쟁하게 되어, 두 번째 라이더부터는 첫 번째 트랜잭션이
    // 끝날 때까지 그대로 대기한다 — 그동안 병렬로 처리될 수 있었던 다른 라이더들의
    // 클레임 요청도 한 줄로 직렬화된다. 나중에 FOR UPDATE SKIP LOCKED로 바꾸면 각자 이미
    // 잠긴 행은 건너뛰고 다음 사용 가능한 행을 잡아가므로 대기 없이 동시에 처리된다.
    fun claimNext(): DispatchQueueRow? =
        jdbcTemplate.query(
            """
            SELECT id, order_id, shop_id, estimated_pickup_at
            FROM delivery
            WHERE status = 'OFFERING'
            ORDER BY created_at ASC
            LIMIT 1
            FOR UPDATE
            """.trimIndent(),
            { rs, _ ->
                DispatchQueueRow(
                    deliveryId = rs.getLong("id"),
                    orderId = rs.getLong("order_id"),
                    shopId = rs.getLong("shop_id"),
                    estimatedPickupAt = rs.getTimestamp("estimated_pickup_at")?.let(Timestamp::toInstant),
                )
            },
        ).firstOrNull()
}
