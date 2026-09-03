package delivery.delivery.api.dto

import delivery.delivery.domain.Delivery

data class DeliveryResponse(
    val deliveryId: Long,
    val status: String,
) {
    companion object {
        fun from(delivery: Delivery): DeliveryResponse =
            DeliveryResponse(deliveryId = delivery.id!!, status = delivery.status.name)
    }
}
