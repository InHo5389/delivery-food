package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.dto.CreateDeliveryCommand
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.Rider
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.RiderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
    private val riderRepository: RiderRepository,
) {
    // order 모듈이 주문 접수(ACCEPTED) 시점에 호출한다. 배차 매칭 스케줄러가 5초마다
    // PENDING 배달을 훑으므로, 이 호출 이후 다음 사이클 안에 오퍼가 나가기 시작한다.
    // estimatedPickupAt은 사장님이 입력한 조리 예상 시간을 "지금부터 N분 후"로 환산한
    // 값이다 — 라이더가 배차 큐에서 이 시각을 보고 언제쯤 출발하면 될지 가늠한다.
    @Transactional
    fun createDelivery(command: CreateDeliveryCommand): Delivery =
        deliveryRepository.save(
            Delivery(
                orderId = command.orderId,
                shopId = command.shopId,
                pickupLatitude = command.pickupLatitude,
                pickupLongitude = command.pickupLongitude,
                estimatedPickupAt = Instant.now().plusSeconds(command.estimatedCookingMinutes * 60L),
            )
        )

    @Transactional
    fun pickup(deliveryId: Long, accountId: Long): Delivery {
        val (delivery, _) = getOwnedDelivery(deliveryId, accountId)
        delivery.transitionTo(DeliveryStatus.PICKED_UP)
        return delivery
    }

    @Transactional
    fun complete(deliveryId: Long, accountId: Long): Delivery {
        val (delivery, rider) = getOwnedDelivery(deliveryId, accountId)
        delivery.transitionTo(DeliveryStatus.DELIVERED)
        rider.goAvailable()
        return delivery
    }

    private fun getOwnedDelivery(deliveryId: Long, accountId: Long): Pair<Delivery, Rider> {
        val delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow { BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND) }
        val rider = riderRepository.findByAccountId(accountId)
            ?: throw BusinessException(DeliveryErrorCode.RIDER_NOT_FOUND)
        if (delivery.riderId != rider.id) {
            throw BusinessException(DeliveryErrorCode.NOT_YOUR_DELIVERY)
        }
        return delivery to rider
    }
}
