package delivery.delivery.application.dto

data class AcceptOfferCommand(
    val offerId: Long,
    val accountId: Long,
)
