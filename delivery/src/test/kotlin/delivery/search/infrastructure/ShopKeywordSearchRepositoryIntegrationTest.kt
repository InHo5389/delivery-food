package delivery.search.infrastructure

import delivery.shop.domain.Shop
import delivery.shop.domain.ShopStatus
import delivery.shop.infrastructure.ShopRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopKeywordSearchRepositoryIntegrationTest(
    @Autowired private val shopRepository: ShopRepository,
    @Autowired private val shopKeywordSearchRepository: ShopKeywordSearchRepository,
) : IntegrationTestSupport() {

    @Test
    fun `상점명에 키워드가 포함되면 검색된다`() {
        val ownerId = System.nanoTime()
        val matched = shopRepository.save(
            Shop(ownerId, "교촌치킨 강남점", "서울", BigDecimal("37.5665000"), BigDecimal("126.9780000"), "0212345678", ShopStatus.OPEN)
        )
        shopRepository.save(
            Shop(ownerId + 1, "김밥천국", "서울", BigDecimal("37.5665000"), BigDecimal("126.9780000"), "0298765432", ShopStatus.OPEN)
        )

        val actual = shopKeywordSearchRepository.searchByKeyword("교촌", limit = 100, offset = 0)

        assertTrue(actual.any { it.id == matched.id })
        assertTrue(actual.none { it.name == "김밥천국" })
    }

    @Test
    fun `상점명 중간에 포함된 키워드도 검색된다`() {
        val ownerId = System.nanoTime()
        val matched = shopRepository.save(
            Shop(ownerId, "강남교촌치킨점", "서울", BigDecimal("37.5665000"), BigDecimal("126.9780000"), "0212345671", ShopStatus.OPEN)
        )

        val actual = shopKeywordSearchRepository.searchByKeyword("교촌", limit = 100, offset = 0)

        assertTrue(actual.any { it.id == matched.id })
    }

    @Test
    fun `영업 종료된 상점은 검색되지 않는다`() {
        val ownerId = System.nanoTime()
        val closed = shopRepository.save(
            Shop(ownerId, "교촌치킨휴업점", "서울", BigDecimal("37.5665000"), BigDecimal("126.9780000"), "0212345672", ShopStatus.CLOSED)
        )

        val actual = shopKeywordSearchRepository.searchByKeyword("교촌", limit = 100, offset = 0)

        assertTrue(actual.none { it.id == closed.id })
    }

    @Test
    fun `일치하는 상점이 없으면 빈 목록을 반환한다`() {
        val actual = shopKeywordSearchRepository.searchByKeyword("존재하지않는키워드XYZ123", limit = 100, offset = 0)

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `limit으로 조회 개수를 제한한다`() {
        val ownerId = System.nanoTime()
        repeat(5) { i ->
            shopRepository.save(
                Shop(ownerId + i, "한정판키워드가게$i", "서울", BigDecimal("37.5665000"), BigDecimal("126.9780000"), "021234500$i", ShopStatus.OPEN)
            )
        }

        val actual = shopKeywordSearchRepository.searchByKeyword("한정판키워드", limit = 2, offset = 0)

        assertEquals(2, actual.size)
    }
}
