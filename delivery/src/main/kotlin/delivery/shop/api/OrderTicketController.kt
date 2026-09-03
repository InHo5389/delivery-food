package delivery.shop.api

import delivery.common.security.AuthenticatedUser
import delivery.shop.api.dto.OrderTicketListResponse
import delivery.shop.application.OrderTicketService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderTicketController(
    private val orderTicketService: OrderTicketService,
) {
    @GetMapping("/order-tickets")
    fun getForShop(
        @RequestParam shopId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): OrderTicketListResponse = OrderTicketListResponse.from(orderTicketService.getForShop(shopId, requester))
}
