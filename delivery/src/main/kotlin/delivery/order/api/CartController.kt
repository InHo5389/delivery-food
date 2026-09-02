package delivery.order.api

import delivery.common.security.AuthenticatedUser
import delivery.order.api.dto.AddCartItemRequest
import delivery.order.api.dto.CartResponse
import delivery.order.api.dto.ChangeCartItemQuantityRequest
import delivery.order.application.CartService
import delivery.order.application.dto.AddCartItemCommand
import delivery.order.application.dto.ChangeCartItemQuantityCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cart")
class CartController(
    private val cartService: CartService,
) {
    @GetMapping
    fun getCart(@AuthenticationPrincipal requester: AuthenticatedUser): CartResponse =
        CartResponse.from(cartService.getCart(requester.userId))

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(
        @Valid @RequestBody request: AddCartItemRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): CartResponse = CartResponse.from(
        cartService.addItem(
            AddCartItemCommand(
                customerId = requester.userId,
                shopId = request.shopId,
                menuId = request.menuId,
                menuName = request.menuName,
                menuPrice = request.menuPrice,
                quantity = request.quantity,
            )
        )
    )

    @PutMapping("/items/{cartItemId}")
    fun changeQuantity(
        @PathVariable cartItemId: Long,
        @Valid @RequestBody request: ChangeCartItemQuantityRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): CartResponse = CartResponse.from(
        cartService.changeQuantity(ChangeCartItemQuantityCommand(requester.userId, cartItemId, request.quantity))
    )

    @DeleteMapping("/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeItem(@PathVariable cartItemId: Long, @AuthenticationPrincipal requester: AuthenticatedUser) {
        cartService.removeItem(requester.userId, cartItemId)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clear(@AuthenticationPrincipal requester: AuthenticatedUser) {
        cartService.clear(requester.userId)
    }
}
