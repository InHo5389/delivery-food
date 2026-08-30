package delivery.search.api

import delivery.search.api.dto.ShopKeywordSearchRequest
import delivery.search.api.dto.ShopKeywordSearchResponse
import delivery.search.application.ShopSearchService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class SearchController(
    private val shopSearchService: ShopSearchService,
) {
    @GetMapping("/shops")
    fun searchShops(@Valid @ModelAttribute request: ShopKeywordSearchRequest): List<ShopKeywordSearchResponse> =
        shopSearchService.searchByKeyword(request.toQuery()).map(ShopKeywordSearchResponse::from)
}
