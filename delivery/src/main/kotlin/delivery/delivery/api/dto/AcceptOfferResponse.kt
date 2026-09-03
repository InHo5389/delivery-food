package delivery.delivery.api.dto

import delivery.delivery.domain.DispatchOffer

data class AcceptOfferResponse(
    val offerId: Long,
    val deliveryId: Long,
    val status: String,
) {
    companion object {
        fun from(offer: DispatchOffer): AcceptOfferResponse =
            AcceptOfferResponse(offerId = offer.id!!, deliveryId = offer.deliveryId, status = offer.status.name)
    }
}
