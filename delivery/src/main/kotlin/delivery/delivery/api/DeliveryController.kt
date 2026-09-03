package delivery.delivery.api

import delivery.common.security.AuthenticatedUser
import delivery.delivery.api.dto.DeliveryResponse
import delivery.delivery.application.DeliveryService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/deliveries")
class DeliveryController(
    private val deliveryService: DeliveryService,
) {
    @PostMapping("/{deliveryId}/pickup")
    fun pickup(
        @PathVariable deliveryId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): DeliveryResponse = DeliveryResponse.from(deliveryService.pickup(deliveryId, requester.userId))

    @PostMapping("/{deliveryId}/complete")
    fun complete(
        @PathVariable deliveryId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): DeliveryResponse = DeliveryResponse.from(deliveryService.complete(deliveryId, requester.userId))
}
