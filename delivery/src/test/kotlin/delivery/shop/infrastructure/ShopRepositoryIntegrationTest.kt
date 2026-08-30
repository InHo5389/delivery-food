package delivery.shop.infrastructure

import delivery.shop.domain.Shop
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShopRepositoryIntegrationTest(
    @Autowired private val shopRepository: ShopRepository,
) : IntegrationTestSupport() {

    @Test
    fun `상점을 저장하면 id가 채번된다`() {
        val shop = Shop(ownerId = 1L, name = "가게", address = "서울", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0212345678")

        val saved = shopRepository.save(shop)

        assertNotNull(saved.id)
    }

    @Test
    fun `ownerId로 상점 목록을 조회한다`() {
        val ownerId = System.nanoTime()
        shopRepository.save(Shop(ownerId = ownerId, name = "가게1", address = "서울", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0212345678"))
        shopRepository.save(Shop(ownerId = ownerId, name = "가게2", address = "부산", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0511111111"))
        shopRepository.save(Shop(ownerId = ownerId + 1, name = "다른사장", address = "인천", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0322222222"))

        val actual = shopRepository.findAllByOwnerId(ownerId)

        assertEquals(2, actual.size)
        assertTrue(actual.all { it.ownerId == ownerId })
    }

    @Test
    fun `존재하지 않는 상점을 조회하면 빈 값을 반환한다`() {
        val actual = shopRepository.findById(Long.MAX_VALUE)

        assertTrue(actual.isEmpty)
    }
}
