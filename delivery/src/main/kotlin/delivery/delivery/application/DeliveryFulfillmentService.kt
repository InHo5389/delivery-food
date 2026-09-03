package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.Rider
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.RiderRepository
import delivery.order.application.OrderService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 픽업/완료는 order 모듈에도 상태 동기화 콜백을 걸어야 해서(delivery → order) 배차 요청
// 생성을 담당하는 DeliveryService와 일부러 분리했다. DeliveryService는 반대 방향
// (order → delivery)으로 쓰이는데, 같은 클래스에 두면 OrderService와 DeliveryService가
// 서로를 주입하는 순환 참조가 생긴다.
@Service
class DeliveryFulfillmentService(
    private val deliveryRepository: DeliveryRepository,
    private val riderRepository: RiderRepository,
    private val orderService: OrderService,
) {
    @Transactional
    fun pickup(deliveryId: Long, accountId: Long): Delivery {
        val (delivery, _) = getOwnedDelivery(deliveryId, accountId)
        delivery.transitionTo(DeliveryStatus.PICKED_UP)
        orderService.markPickedUp(delivery.orderId)
        return delivery
    }

    @Transactional
    fun complete(deliveryId: Long, accountId: Long): Delivery {
        val (delivery, rider) = getOwnedDelivery(deliveryId, accountId)
        delivery.transitionTo(DeliveryStatus.DELIVERED)
        rider.goAvailable()
        orderService.markDelivered(delivery.orderId)
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
