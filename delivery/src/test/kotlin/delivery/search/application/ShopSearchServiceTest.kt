package delivery.search.application

import delivery.search.application.dto.ShopKeywordSearchQuery
import delivery.search.infrastructure.ShopKeywordSearchRepository
import delivery.search.infrastructure.ShopKeywordSearchRow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopSearchServiceTest {

    private val shopKeywordSearchRepository = mockk<ShopKeywordSearchRepository>()
    private lateinit var shopSearchService: ShopSearchService

    @BeforeEach
    fun setUp() {
        shopSearchService = ShopSearchService(shopKeywordSearchRepository)
    }

    @Test
    fun `키워드로 검색하면 결과를 반환한다`() {
        val query = ShopKeywordSearchQuery(keyword = "교촌", limit = 20, offset = 0)
        every { shopKeywordSearchRepository.searchByKeyword("교촌", 20, 0) } returns listOf(
            ShopKeywordSearchRow(id = 1L, name = "교촌치킨", address = "서울"),
        )

        val actual = shopSearchService.searchByKeyword(query)

        assertEquals(1, actual.size)
        assertEquals("교촌치킨", actual[0].name)
    }

    @Test
    fun `검색 결과가 없으면 빈 목록을 반환한다`() {
        val query = ShopKeywordSearchQuery(keyword = "없는가게", limit = 20, offset = 0)
        every { shopKeywordSearchRepository.searchByKeyword("없는가게", 20, 0) } returns emptyList()

        val actual = shopSearchService.searchByKeyword(query)

        assertTrue(actual.isEmpty())
    }
}
