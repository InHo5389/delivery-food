package delivery.delivery.infrastructure

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class DeliveryAssignmentRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // 동시 수락 경쟁의 실제 승부처. 별도 락 없이 단일 UPDATE의 원자성에 기댄다 —
    // "OFFERING이고 아직 배정 안 된 배달"이라는 조건을 이 UPDATE 문 안에서 함께 재확인하므로,
    // 같은 배달에 여러 라이더가 동시에 이 메서드를 호출해도 정확히 하나의 UPDATE만
    // rider_id를 바꾸는 데 성공하고 나머지는 WHERE 조건에 걸려 0행을 갱신한다.
    fun tryAssignRider(deliveryId: Long, riderId: Long): Boolean {
        val updatedRows = jdbcTemplate.update(
            """
            UPDATE delivery
            SET rider_id = ?, status = 'ASSIGNED', updated_at = ?
            WHERE id = ? AND status = 'OFFERING' AND rider_id IS NULL
            """.trimIndent(),
            riderId, Timestamp.from(Instant.now()), deliveryId,
        )
        return updatedRows > 0
    }
}
