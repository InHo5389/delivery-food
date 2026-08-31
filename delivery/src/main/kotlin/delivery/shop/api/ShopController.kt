package delivery.shop.api

import delivery.common.security.AuthenticatedUser
import delivery.shop.api.dto.CreateShopRequest
import delivery.shop.api.dto.NearbyShopRequest
import delivery.shop.api.dto.NearbyShopResponse
import delivery.shop.api.dto.ShopDetailResponse
import delivery.shop.api.dto.ShopResponse
import delivery.shop.api.dto.UpdateShopRequest
import delivery.shop.application.ShopService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/shops")
class ShopController(
    private val shopService: ShopService,
) {
    @GetMapping
    fun getNearbyShops(@Valid @ModelAttribute request: NearbyShopRequest): List<NearbyShopResponse> =
        shopService.getNearbyOpenShops(request.toQuery()).map(NearbyShopResponse::from)

    @GetMapping("/{shopId}")
    fun getShopDetail(@PathVariable shopId: Long): ShopDetailResponse =
        ShopDetailResponse.from(shopService.getShopDetail(shopId))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateShopRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): ShopResponse = ShopResponse.from(shopService.create(request.toCommand(), requester))

    @PutMapping("/{shopId}")
    fun update(
        @PathVariable shopId: Long,
        @Valid @RequestBody request: UpdateShopRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): ShopResponse = ShopResponse.from(shopService.update(shopId, request.toCommand(), requester))

    @PostMapping("/{shopId}/open")
    fun open(@PathVariable shopId: Long, @AuthenticationPrincipal requester: AuthenticatedUser) {
        shopService.open(shopId, requester)
    }

    @PostMapping("/{shopId}/close")
    fun close(@PathVariable shopId: Long, @AuthenticationPrincipal requester: AuthenticatedUser) {
        shopService.close(shopId, requester)
    }

    @DeleteMapping("/{shopId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable shopId: Long, @AuthenticationPrincipal requester: AuthenticatedUser) {
        shopService.delete(shopId, requester)
    }
}
