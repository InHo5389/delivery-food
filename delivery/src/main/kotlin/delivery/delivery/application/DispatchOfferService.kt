package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.dto.AcceptOfferCommand
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DispatchOffer
import delivery.delivery.domain.DispatchOfferStatus
import delivery.delivery.infrastructure.DeliveryAssignmentRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.RiderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DispatchOfferService(
    private val dispatchOfferRepository: DispatchOfferRepository,
    private val riderRepository: RiderRepository,
    private val deliveryAssignmentRepository: DeliveryAssignmentRepository,
) {
    @Transactional
    fun accept(command: AcceptOfferCommand): DispatchOffer {
        val offer = dispatchOfferRepository.findById(command.offerId)
            .orElseThrow { BusinessException(DeliveryErrorCode.OFFER_NOT_FOUND) }
        val rider = riderRepository.findByAccountId(command.accountId)
            ?: throw BusinessException(DeliveryErrorCode.RIDER_NOT_FOUND)
        if (offer.riderId != rider.id) {
            throw BusinessException(DeliveryErrorCode.NOT_YOUR_OFFER)
        }

        val assigned = deliveryAssignmentRepository.tryAssignRider(offer.deliveryId, rider.id!!)
        if (!assigned) {
            throw BusinessException(DeliveryErrorCode.DISPATCH_ALREADY_ASSIGNED)
        }

        offer.status = DispatchOfferStatus.ACCEPTED
        offer.respondedAt = Instant.now()
        rejectOtherOffers(offer.deliveryId, offer.id!!)
        rider.goBusy()

        return offer
    }

    private fun rejectOtherOffers(deliveryId: Long, acceptedOfferId: Long) {
        dispatchOfferRepository.findAllByDeliveryId(deliveryId)
            .filter { it.id != acceptedOfferId && it.status == DispatchOfferStatus.SENT }
            .forEach {
                it.status = DispatchOfferStatus.REJECTED
                it.respondedAt = Instant.now()
            }
    }
}
