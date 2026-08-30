package delivery.shop.api

import delivery.shop.api.dto.NearbyShopRequest
import delivery.shop.api.dto.NearbyShopResponse
import delivery.shop.api.dto.ShopDetailResponse
import delivery.shop.application.ShopService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
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
}
