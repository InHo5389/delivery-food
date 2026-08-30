package delivery.shop.application

import delivery.common.exception.BusinessException
import delivery.shop.application.dto.CreateShopCommand
import delivery.shop.application.dto.NearbyShopQuery
import delivery.shop.application.dto.UpdateShopCommand
import delivery.shop.infrastructure.NearbyShopRow
import delivery.shop.domain.Shop
import delivery.shop.domain.ShopErrorCode
import delivery.shop.domain.ShopStatus
import delivery.shop.infrastructure.ShopRepository
import delivery.shop.infrastructure.ShopSearchRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopServiceTest {

    private val shopRepository = mockk<ShopRepository>()
    private val shopSearchRepository = mockk<ShopSearchRepository>()
    private lateinit var shopService: ShopService

    private val latitude = BigDecimal("37.5665000")
    private val longitude = BigDecimal("126.9780000")

    @BeforeEach
    fun setUp() {
        shopService = ShopService(shopRepository, shopSearchRepository)
    }

    @Test
    fun `상점을 생성하면 저장한다`() {
        val command = CreateShopCommand(ownerId = 1L, name = "가게", address = "서울", latitude = latitude, longitude = longitude, phone = "0212345678")
        val savedSlot = slot<Shop>()
        every { shopRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val actual = shopService.create(command)

        assertEquals("가게", actual.name)
        assertEquals(1L, savedSlot.captured.ownerId)
    }

    @Test
    fun `존재하지 않는 상점을 조회하면 예외가 발생한다`() {
        every { shopRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { shopService.getById(999L) }

        assertEquals(ShopErrorCode.SHOP_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `존재하는 상점을 조회하면 반환한다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", latitude, longitude)
        every { shopRepository.findById(1L) } returns Optional.of(shop)

        val actual = shopService.getById(1L)

        assertEquals(1L, actual.id)
    }

    @Test
    fun `상점 정보를 수정하면 필드가 반영된다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", latitude, longitude)
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        val command = UpdateShopCommand(name = "새이름", address = "부산", latitude = latitude, longitude = longitude, phone = "0511111111")

        val actual = shopService.update(1L, command)

        assertEquals("새이름", actual.name)
        assertEquals("부산", actual.address)
        assertEquals("0511111111", actual.phone)
    }

    @Test
    fun `open을 호출하면 상점 상태가 OPEN이 된다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", latitude, longitude, ShopStatus.CLOSED)
        every { shopRepository.findById(1L) } returns Optional.of(shop)

        shopService.open(1L)

        assertTrue(shop.isOpen())
    }

    @Test
    fun `close를 호출하면 상점 상태가 CLOSED가 된다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", latitude, longitude, ShopStatus.OPEN)
        every { shopRepository.findById(1L) } returns Optional.of(shop)

        shopService.close(1L)

        assertFalse(shop.isOpen())
    }

    @Test
    fun `주변 영업중 상점을 거리순으로 조회한다`() {
        val query = NearbyShopQuery(latitude = 37.5, longitude = 127.0, limit = 20, offset = 0)
        every { shopSearchRepository.findNearbyOpenShops(37.5, 127.0, 20, 0) } returns listOf(
            NearbyShopRow(id = 1L, name = "가까운가게", address = "서울", distanceMeters = 120.5),
            NearbyShopRow(id = 2L, name = "먼가게", address = "서울", distanceMeters = 980.2),
        )

        val actual = shopService.getNearbyOpenShops(query)

        assertEquals(2, actual.size)
        assertEquals("가까운가게", actual[0].name)
        assertEquals(120.5, actual[0].distanceMeters)
    }

    @Test
    fun `주변에 영업중 상점이 없으면 빈 목록을 반환한다`() {
        val query = NearbyShopQuery(latitude = 37.5, longitude = 127.0, limit = 20, offset = 0)
        every { shopSearchRepository.findNearbyOpenShops(37.5, 127.0, 20, 0) } returns emptyList()

        val actual = shopService.getNearbyOpenShops(query)

        assertTrue(actual.isEmpty())
    }
}
