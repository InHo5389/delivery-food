package delivery.delivery.api

import delivery.common.security.AuthenticatedUser
import delivery.delivery.api.dto.AcceptOfferResponse
import delivery.delivery.application.DispatchOfferService
import delivery.delivery.application.dto.AcceptOfferCommand
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dispatch-offers")
class DispatchOfferController(
    private val dispatchOfferService: DispatchOfferService,
) {
    @PostMapping("/{offerId}/accept")
    fun accept(
        @PathVariable offerId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): AcceptOfferResponse =
        AcceptOfferResponse.from(dispatchOfferService.accept(AcceptOfferCommand(offerId, requester.userId)))
}
