package delivery.shop.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ShopSearchRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // ⚠️ 의도적 구식 구현 — Phase 3 A-1에서 Geohash + Redis GEO로 개선 예정.
    //   ST_Distance_Sphere를 ORDER BY에 직접 사용하면 인덱스를 타지 못해 풀스캔이 발생한다.
    // ⚠️ SELECT * (전체 컬럼, 의도적, A-7에서 DTO 프로젝션으로 개선 예정)
    fun findNearbyOpenShops(latitude: Double, longitude: Double, limit: Int, offset: Int): List<NearbyShopRow> =
        jdbcTemplate.query(
            """
            SELECT *,
                   ST_Distance_Sphere(
                       POINT(longitude, latitude),
                       POINT(?, ?)
                   ) AS distance_meters
            FROM shop
            WHERE status = 'OPEN'
            ORDER BY distance_meters ASC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            { rs, _ ->
                NearbyShopRow(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    address = rs.getString("address"),
                    distanceMeters = rs.getDouble("distance_meters"),
                )
            },
            longitude, latitude, limit, offset,
        )
}
