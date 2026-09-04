package delivery.delivery.infrastructure

import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface DeliveryRepository : JpaRepository<Delivery, Long> {
    // 배차 스케줄러가 매 사이클마다 배차 대기 중인 배달을 찾는 진입점.
    fun findAllByStatus(status: DeliveryStatus): List<Delivery>

    // 라이더 정산이 "이 라이더가 이 기간에 완료한 배달"을 찾는 조회.
    fun findAllByRiderIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
        riderId: Long,
        status: DeliveryStatus,
        from: Instant,
        to: Instant,
    ): List<Delivery>
}
