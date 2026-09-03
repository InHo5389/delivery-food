package delivery.delivery.infrastructure

import delivery.delivery.domain.DispatchOffer
import org.springframework.data.jpa.repository.JpaRepository

interface DispatchOfferRepository : JpaRepository<DispatchOffer, Long> {
    // 다음 배차 사이클에서 이미 오퍼를 받은 라이더를 후보에서 제외하기 위한 조회.
    fun findAllByDeliveryId(deliveryId: Long): List<DispatchOffer>
}
