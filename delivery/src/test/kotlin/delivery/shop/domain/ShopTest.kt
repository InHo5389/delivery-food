package delivery.shop.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopTest {

    @Test
    fun `신규 생성 시 id는 null이다`() {
        val shop = Shop(ownerId = 1L, name = "가게", address = "서울", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0212345678")

        assertNull(shop.id)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val shop = Shop.withId(id = 10L, ownerId = 1L, name = "가게", address = "서울", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0212345678")

        assertEquals(10L, shop.id)
        assertEquals(1L, shop.ownerId)
    }

    @Test
    fun `신규 생성 시 기본 상태는 CLOSED이다`() {
        val shop = Shop(ownerId = 1L, name = "가게", address = "서울", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0212345678")

        assertEquals(ShopStatus.CLOSED, shop.status)
        assertFalse(shop.isOpen())
    }

    @Test
    fun `open을 호출하면 상태가 OPEN으로 바뀐다`() {
        val shop = Shop(ownerId = 1L, name = "가게", address = "서울", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0212345678")

        shop.open()

        assertEquals(ShopStatus.OPEN, shop.status)
        assertTrue(shop.isOpen())
    }

    @Test
    fun `close를 호출하면 상태가 CLOSED로 바뀐다`() {
        val shop = Shop(ownerId = 1L, name = "가게", address = "서울", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0212345678")
        shop.open()

        shop.close()

        assertEquals(ShopStatus.CLOSED, shop.status)
        assertFalse(shop.isOpen())
    }
}
