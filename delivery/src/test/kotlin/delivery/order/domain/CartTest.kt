package delivery.order.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CartTest {

    @Test
    fun `신규 생성 시 id는 null이다`() {
        val cart = Cart(customerId = 1L, shopId = 1L)

        assertNull(cart.id)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val cart = Cart.withId(id = 10L, customerId = 1L, shopId = 1L)

        assertEquals(10L, cart.id)
        assertEquals(1L, cart.customerId)
        assertEquals(1L, cart.shopId)
    }

    @Test
    fun `replaceShop을 호출하면 shopId가 바뀐다`() {
        val cart = Cart(customerId = 1L, shopId = 1L)

        cart.replaceShop(2L)

        assertEquals(2L, cart.shopId)
    }
}
