package delivery.order.api

import delivery.common.security.AuthenticatedUser
import delivery.order.api.dto.CreateOrderRequest
import delivery.order.api.dto.CreateOrderResponse
import delivery.order.api.dto.OrderHistoryRequest
import delivery.order.api.dto.OrderHistoryResponse
import delivery.order.api.dto.OrderResponse
import delivery.order.application.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): CreateOrderResponse = CreateOrderResponse.from(orderService.createOrder(request.toCommand(requester.userId)))

    // ⚠️ 의도적 구식 구현 — Phase 3 A-3에서 커서 기반 페이징으로 개선 예정(OrderService 참조).
    @GetMapping
    fun getMyOrders(
        @Valid @ModelAttribute request: OrderHistoryRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): OrderHistoryResponse = OrderHistoryResponse.from(orderService.getMyOrderHistory(request.toQuery(requester.userId)))

    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable orderId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): OrderResponse = OrderResponse.from(orderService.getOrder(orderId, requester))
}
