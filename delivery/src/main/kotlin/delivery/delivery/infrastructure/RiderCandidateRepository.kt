package delivery.delivery.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class RiderCandidateRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // ⚠️ 의도적 구식 구현 — 상점 목록 조회와 동일한 트레이드오프.
    //   ST_Distance_Sphere를 HAVING에 직접 써서 반경을 걸러내면 인덱스를 타지 못해
    //   AVAILABLE 라이더 전체를 스캔한다. Phase 3에서 Redis GEO로 개선 예정.
    fun findAvailableCandidates(
        pickupLatitude: Double,
        pickupLongitude: Double,
        radiusMeters: Double,
    ): List<RiderCandidateRow> =
        jdbcTemplate.query(
            """
            SELECT id, recent_delivery_count, acceptance_rate, available_since,
                   ST_Distance_Sphere(
                       POINT(longitude, latitude),
                       POINT(?, ?)
                   ) AS distance_meters
            FROM rider
            WHERE status = 'AVAILABLE'
            HAVING distance_meters <= ?
            """.trimIndent(),
            { rs, _ ->
                RiderCandidateRow(
                    riderId = rs.getLong("id"),
                    distanceMeters = rs.getDouble("distance_meters"),
                    recentDeliveryCount = rs.getInt("recent_delivery_count"),
                    acceptanceRate = rs.getBigDecimal("acceptance_rate").toDouble(),
                    availableSince = rs.getTimestamp("available_since")?.let(Timestamp::toInstant),
                )
            },
            pickupLongitude, pickupLatitude, radiusMeters,
        )
}
