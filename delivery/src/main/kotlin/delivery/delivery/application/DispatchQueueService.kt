package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.dto.DispatchQueueItem
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DispatchOfferStatus
import delivery.delivery.domain.RiderStatus
import delivery.delivery.infrastructure.DeliveryAssignmentRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.DispatchQueueRepository
import delivery.delivery.infrastructure.RiderRepository
import delivery.order.application.OrderService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DispatchQueueService(
    private val dispatchQueueRepository: DispatchQueueRepository,
    private val riderRepository: RiderRepository,
    private val deliveryAssignmentRepository: DeliveryAssignmentRepository,
    private val dispatchOfferRepository: DispatchOfferRepository,
    private val orderService: OrderService,
) {
    fun getQueue(limit: Int): List<DispatchQueueItem> =
        dispatchQueueRepository.findQueue(limit).map {
            DispatchQueueItem(deliveryId = it.deliveryId, orderId = it.orderId, shopId = it.shopId, estimatedPickupAt = it.estimatedPickupAt)
        }

    // 큐 맨 앞 배달을 잠근 채로(claimNext) 그 안에서 바로 배정까지 끝내야 "보고 나서
    // 다른 라이더에게 뺏기는" 경쟁 상태가 생기지 않는다. 그래서 SELECT와 UPDATE를
    // 하나의 트랜잭션(따라서 같은 커넥션)에 묶어야 하고, @Transactional이 그걸 보장한다.
    @Transactional
    fun claim(accountId: Long): DispatchQueueItem {
        val rider = riderRepository.findByAccountId(accountId)
            ?: throw BusinessException(DeliveryErrorCode.RIDER_NOT_FOUND)
        if (rider.status != RiderStatus.AVAILABLE) {
            throw BusinessException(DeliveryErrorCode.RIDER_NOT_AVAILABLE)
        }

        val next = dispatchQueueRepository.claimNext()
            ?: throw BusinessException(DeliveryErrorCode.DISPATCH_QUEUE_EMPTY)

        // claimNext()가 이미 이 행을 잠근 채로 돌려줬으므로 이 UPDATE가 실패할 일은
        // 없어야 하지만, SELECT와 UPDATE를 나중에 분리하는 실수를 방지하기 위해
        // 조건은 그대로 남겨 방어적으로 검증한다.
        val assigned = deliveryAssignmentRepository.tryAssignRider(next.deliveryId, rider.id!!)
        if (!assigned) {
            throw BusinessException(DeliveryErrorCode.DISPATCH_ALREADY_ASSIGNED)
        }

        expirePendingOffers(next.deliveryId)
        rider.goBusy()
        orderService.markRiderAssigned(next.orderId)

        return DispatchQueueItem(deliveryId = next.deliveryId, orderId = next.orderId, shopId = next.shopId, estimatedPickupAt = next.estimatedPickupAt)
    }

    // 이 배달이 개별 오퍼(커밋 42-44 경로)로도 다른 라이더들에게 나가 있었을 수 있다.
    // 큐 클레임으로 이미 배정이 끝났으니 그 오퍼들은 응답을 받아도 의미가 없어져
    // EXPIRED로 정리한다(라이더가 직접 거절한 게 아니라서 REJECTED와 구분한다).
    private fun expirePendingOffers(deliveryId: Long) {
        dispatchOfferRepository.findAllByDeliveryId(deliveryId)
            .filter { it.status == DispatchOfferStatus.SENT }
            .forEach {
                it.status = DispatchOfferStatus.EXPIRED
                it.respondedAt = Instant.now()
            }
    }
}
