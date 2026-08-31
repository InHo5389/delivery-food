package delivery.search.application

import delivery.search.application.dto.ShopKeywordSearchQuery
import delivery.search.application.dto.ShopKeywordSearchResult
import delivery.search.infrastructure.ShopKeywordSearchRepository
import org.springframework.stereotype.Service

@Service
class ShopSearchService(
    private val shopKeywordSearchRepository: ShopKeywordSearchRepository,
) {
    fun searchByKeyword(query: ShopKeywordSearchQuery): List<ShopKeywordSearchResult> =
        shopKeywordSearchRepository.searchByKeyword(query.keyword, query.limit, query.offset)
            .map { ShopKeywordSearchResult(it.id, it.name, it.address, it.minOrderAmount, it.deliveryFee) }
}
