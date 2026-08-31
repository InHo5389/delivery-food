package delivery.shop.infrastructure

import delivery.shop.domain.Shop
import delivery.shop.domain.ShopStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopSearchRepositoryIntegrationTest(
    @Autowired private val shopRepository: ShopRepository,
    @Autowired private val shopSearchRepository: ShopSearchRepository,
) : IntegrationTestSupport() {

    // 서울시청 좌표를 기준점으로 사용
    private val originLat = 37.5665
    private val originLng = 126.9780

    @Test
    fun `영업중인 상점만 거리순으로 조회한다`() {
        val ownerId = System.nanoTime()
        val near = shopRepository.save(
            Shop(ownerId, "가까운가게", "서울", BigDecimal("37.5670000"), BigDecimal("126.9785000"), "0212345678", status = ShopStatus.OPEN)
        )
        val far = shopRepository.save(
            Shop(ownerId, "먼가게", "부산", BigDecimal("35.1796000"), BigDecimal("129.0756000"), "0512345678", status = ShopStatus.OPEN)
        )
        shopRepository.save(
            Shop(ownerId, "영업안함", "서울", BigDecimal("37.5666000"), BigDecimal("126.9781000"), "0298765432", status = ShopStatus.CLOSED)
        )

        val actual = shopSearchRepository.findNearbyOpenShops(originLat, originLng, limit = 100, offset = 0)

        val nearIndex = actual.indexOfFirst { it.id == near.id }
        val farIndex = actual.indexOfFirst { it.id == far.id }
        assertTrue(nearIndex in actual.indices)
        assertTrue(farIndex in actual.indices)
        assertTrue(nearIndex < farIndex)
        assertTrue(actual[nearIndex].distanceMeters < actual[farIndex].distanceMeters)
    }

    @Test
    fun `영업 종료 상점은 결과에서 제외된다`() {
        val ownerId = System.nanoTime()
        val closed = shopRepository.save(
            Shop(ownerId, "영업안함", "서울", BigDecimal("37.5666000"), BigDecimal("126.9781000"), "0298765432", status = ShopStatus.CLOSED)
        )

        val actual = shopSearchRepository.findNearbyOpenShops(originLat, originLng, limit = 100, offset = 0)

        assertTrue(actual.none { it.id == closed.id })
    }

    @Test
    fun `limit으로 조회 개수를 제한한다`() {
        val ownerId = System.nanoTime()
        repeat(5) { i ->
            shopRepository.save(
                Shop(ownerId + i, "가게$i", "서울", BigDecimal("37.566${i}000"), BigDecimal("126.978${i}000"), "021234567$i", status = ShopStatus.OPEN)
            )
        }

        val actual = shopSearchRepository.findNearbyOpenShops(originLat, originLng, limit = 2, offset = 0)

        assertEquals(2, actual.size)
    }

    @Test
    fun `offset으로 다음 페이지를 조회한다`() {
        val ownerId = System.nanoTime()
        repeat(3) { i ->
            shopRepository.save(
                Shop(ownerId + i, "페이지가게$i", "서울", BigDecimal("37.566${i}000"), BigDecimal("126.978${i}000"), "031234567$i", status = ShopStatus.OPEN)
            )
        }

        val firstPage = shopSearchRepository.findNearbyOpenShops(originLat, originLng, limit = 2, offset = 0)
        val secondPage = shopSearchRepository.findNearbyOpenShops(originLat, originLng, limit = 2, offset = 2)

        assertEquals(2, firstPage.size)
        assertTrue(secondPage.isNotEmpty())
        assertTrue(firstPage.none { first -> secondPage.any { it.id == first.id } })
    }
}
