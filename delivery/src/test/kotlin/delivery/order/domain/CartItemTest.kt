package delivery.order.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CartItemTest {

    @Test
    fun `신규 생성 시 id는 null이다`() {
        val item = CartItem(cartId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1)

        assertNull(item.id)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val item = CartItem.withId(id = 5L, cartId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1)

        assertEquals(5L, item.id)
    }

    @Test
    fun `changeQuantity를 호출하면 수량이 바뀐다`() {
        val item = CartItem(cartId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1)

        item.changeQuantity(3)

        assertEquals(3, item.quantity)
    }
}
