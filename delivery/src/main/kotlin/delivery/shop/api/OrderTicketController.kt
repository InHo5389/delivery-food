package delivery.shop.api

import delivery.common.security.AuthenticatedUser
import delivery.order.application.OrderService
import delivery.shop.api.dto.OrderTicketListResponse
import delivery.shop.api.dto.OrderTicketResponse
import delivery.shop.application.OrderTicketService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// shop이 소유한 티켓(OrderTicketService)과 order가 소유한 항목(OrderService)을
// 이 컨트롤러가 조합해서 응답을 만든다. shop.application이 order를 직접 호출하게
// 두면 order → shop 방향과 맞물려 순환 참조가 생기므로, 두 서비스 다 의존하지 않는
// api 계층에서 조합한다(Facade는 안 쓰지만, 서로 다른 두 모듈 서비스를 한 번에
// 쓰는 조회 화면은 이렇게 컨트롤러가 조립).
@RestController
class OrderTicketController(
    private val orderTicketService: OrderTicketService,
    private val orderService: OrderService,
) {
    @GetMapping("/order-tickets")
    fun getForShop(
        @RequestParam shopId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): OrderTicketListResponse {
        val tickets = orderTicketService.getForShop(shopId, requester).map { ticket ->
            OrderTicketResponse.from(ticket, orderService.getOrderItemSummaries(ticket.orderId))
        }
        return OrderTicketListResponse(tickets)
    }
}
