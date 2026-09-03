package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.dto.DispatchCycleResult
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.DispatchCandidate
import delivery.delivery.domain.DispatchOffer
import delivery.delivery.domain.DispatchScorer
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.RiderCandidateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

// 상점 조회의 기본 반경(ShopSearchRepository.DEFAULT_RADIUS_METERS)과 동일한 3km —
// 배달 가능 범위를 정하는 기준이 상점 노출 범위와 다르면 "보이는데 배차가 안 되는" 상점이 생긴다.
private const val DISPATCH_RADIUS_METERS = 3000.0

// 배차 1건당 동시에 오퍼를 보낼 라이더 수(상위 N명). 너무 크면 여러 라이더가 같은 건을 두고
// 헛수고를 하고(수락 경쟁은 CAS로 막을 예정이지만, 나머지는 거절/무응답 처리만 남는다),
// 너무 작으면 전원 무응답 시 다음 사이클까지 재시도가 지연된다.
private const val OFFER_COUNT = 3

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

    @Transactional
    fun dispatchOne(deliveryId: Long): DispatchCycleResult {
        val delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow { BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND) }

        val alreadyOfferedRiderIds = dispatchOfferRepository.findAllByDeliveryId(deliveryId)
            .map { it.riderId }
            .toSet()

        val now = Instant.now()
        val candidates = riderCandidateRepository.findAvailableCandidates(
            pickupLatitude = delivery.pickupLatitude.toDouble(),
            pickupLongitude = delivery.pickupLongitude.toDouble(),
            radiusMeters = DISPATCH_RADIUS_METERS,
        )
            .filterNot { it.riderId in alreadyOfferedRiderIds }
            .map {
                DispatchCandidate(
                    riderId = it.riderId,
                    distanceMeters = it.distanceMeters,
                    recentDeliveryCount = it.recentDeliveryCount,
                    acceptanceRate = it.acceptanceRate,
                    waitSeconds = Duration.between(it.availableSince ?: now, now).seconds.coerceAtLeast(0),
                )
            }

        if (candidates.isEmpty()) {
            return DispatchCycleResult(deliveryId, emptyList())
        }

        val scores = DispatchScorer.score(candidates)
        val selected = candidates.sortedByDescending { scores.getValue(it.riderId) }.take(OFFER_COUNT)

        selected.forEach { candidate ->
            dispatchOfferRepository.save(
                DispatchOffer(
                    deliveryId = deliveryId,
                    riderId = candidate.riderId,
                    score = scores.getValue(candidate.riderId),
                )
            )
        }

        if (delivery.status == DeliveryStatus.PENDING) {
            delivery.transitionTo(DeliveryStatus.OFFERING)
        }

        return DispatchCycleResult(deliveryId, selected.map { it.riderId })
    }
}
