package delivery.shop.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

// 배차 반경(가이드 문서 커밋 42, 반경 3km)과 동일하게 맞춘 기본 배달 가능 반경.
private const val DEFAULT_RADIUS_METERS = 3000.0

@Repository
class ShopSearchRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // ⚠️ 의도적 구식 구현 — Phase 3 A-1에서 Geohash + Redis GEO로 개선 예정.
    //   ST_Distance_Sphere를 WHERE/ORDER BY에 직접 사용하면 인덱스를 타지 못해 풀스캔이 발생한다.
    // ⚠️ SELECT * (전체 컬럼, 의도적, A-7에서 DTO 프로젝션으로 개선 예정)
    fun findNearbyOpenShops(
        latitude: Double,
        longitude: Double,
        limit: Int,
        offset: Int,
        radiusMeters: Double = DEFAULT_RADIUS_METERS,
    ): List<NearbyShopRow> =
        jdbcTemplate.query(
            """
            SELECT *,
                   ST_Distance_Sphere(
                       POINT(longitude, latitude),
                       POINT(?, ?)
                   ) AS distance_meters
            FROM shop
            WHERE status = 'OPEN'
            HAVING distance_meters <= ?
            ORDER BY distance_meters ASC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            { rs, _ ->
                NearbyShopRow(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    address = rs.getString("address"),
                    minOrderAmount = rs.getLong("min_order_amount"),
                    deliveryFee = rs.getLong("delivery_fee"),
                    distanceMeters = rs.getDouble("distance_meters"),
                )
            },
            longitude, latitude, radiusMeters, limit, offset,
        )
}
