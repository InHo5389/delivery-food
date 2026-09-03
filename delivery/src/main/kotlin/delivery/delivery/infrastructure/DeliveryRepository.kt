package delivery.delivery.infrastructure

import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository

interface DeliveryRepository : JpaRepository<Delivery, Long> {
    // 배차 스케줄러가 매 사이클마다 배차 대기 중인 배달을 찾는 진입점.
    fun findAllByStatus(status: DeliveryStatus): List<Delivery>
}
