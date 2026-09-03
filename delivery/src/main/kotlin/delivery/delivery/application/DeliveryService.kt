package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.Rider
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.RiderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
    private val riderRepository: RiderRepository,
) {
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
