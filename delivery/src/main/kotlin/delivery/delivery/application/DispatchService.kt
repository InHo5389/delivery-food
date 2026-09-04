package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.dto.DispatchCycleResult
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.DispatchOffer
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.RiderCandidateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 상점 조회의 기본 반경(ShopSearchRepository.DEFAULT_RADIUS_METERS)과 동일한 3km —
// 배달 가능 범위를 정하는 기준이 상점 노출 범위와 다르면 "보이는데 배차가 안 되는" 상점이 생긴다.
private const val DISPATCH_RADIUS_METERS = 3000.0

@Service
class DispatchService(
    private val deliveryRepository: DeliveryRepository,
    private val riderCandidateRepository: RiderCandidateRepository,
    private val dispatchOfferRepository: DispatchOfferRepository,
) {
    private val logger = LoggerFactory.getLogger(DispatchService::class.java)

    // 스케줄러(DispatchScheduler)의 진입점. 배달 단위로 트랜잭션을 나눠(dispatchOne)
    // 한 건에서 예외가 나도 같은 사이클의 다른 배달 배차까지 막지 않도록 한다.
    fun runDispatchCycle(): List<DispatchCycleResult> =
        deliveryRepository.findAllByStatus(DeliveryStatus.PENDING).mapNotNull { delivery ->
            runCatching { dispatchOne(delivery.id!!) }
                .onFailure { logger.error("배차 실패: deliveryId={}", delivery.id, it) }
                .getOrNull()
        }

    // 반경 안의 AVAILABLE 라이더 전원에게 오퍼를 보낸다 — 누가 먼저 수락하는지로만 정해지는
    // 선착순 방식이라 후보를 줄 세울 기준이 필요 없다. 실제 배정은 이후 수락 API의
    // 조건부 UPDATE(CAS)가 정확히 한 명만 성공시킨다.
    @Transactional
    fun dispatchOne(deliveryId: Long): DispatchCycleResult {
        val delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow { BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND) }

        val alreadyOfferedRiderIds = dispatchOfferRepository.findAllByDeliveryId(deliveryId)
            .map { it.riderId }
            .toSet()

        val candidateRiderIds = riderCandidateRepository.findAvailableCandidates(
            pickupLatitude = delivery.pickupLatitude.toDouble(),
            pickupLongitude = delivery.pickupLongitude.toDouble(),
            radiusMeters = DISPATCH_RADIUS_METERS,
        )
            .map { it.riderId }
            .filterNot { it in alreadyOfferedRiderIds }

        if (candidateRiderIds.isEmpty()) {
            return DispatchCycleResult(deliveryId, emptyList())
        }

        candidateRiderIds.forEach { riderId ->
            dispatchOfferRepository.save(DispatchOffer(deliveryId = deliveryId, riderId = riderId))
        }

        if (delivery.status == DeliveryStatus.PENDING) {
            delivery.transitionTo(DeliveryStatus.OFFERING)
        }

        return DispatchCycleResult(deliveryId, candidateRiderIds)
    }
}
