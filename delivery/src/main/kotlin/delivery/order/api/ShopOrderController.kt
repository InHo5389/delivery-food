package delivery.order.api

import delivery.common.security.AuthenticatedUser
import delivery.order.api.dto.ShopOrderListResponse
import delivery.order.api.dto.ShopOrderResponse
import delivery.order.application.OrderService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 사장님의 주문표(주방 화면) 조회 전용 컨트롤러. /orders는 고객 관점(내 주문)이라
// OrderController(class-level @RequestMapping("/orders"))와 URL 프리픽스를 공유하지
// 않는다 — 예전 OrderTicketController(shop 모듈)가 쓰던 경로(/order-tickets)를
// 그대로 유지한다.
@RestController
class ShopOrderController(
    private val orderService: OrderService,
) {
    @GetMapping("/order-tickets")
    fun getOrdersForShop(
        @RequestParam shopId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): ShopOrderListResponse =
        ShopOrderListResponse(orderService.getOrdersForShop(shopId, requester).map(ShopOrderResponse::from))
}
