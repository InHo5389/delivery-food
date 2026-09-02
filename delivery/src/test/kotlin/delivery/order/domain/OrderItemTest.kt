package delivery.order.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrderItemTest {

    private fun newItem(): OrderItem = OrderItem(
        orderId = 1L,
        menuId = 1L,
        menuName = "짜장면",
        menuPrice = 8000L,
        quantity = 2,
    )

    @Test
    fun `신규 생성 시 id는 null이다`() {
        val item = newItem()

        assertNull(item.id)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val item = OrderItem.withId(id = 10L, orderId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 2)

        assertEquals(10L, item.id)
    }

    @Test
    fun `메뉴명과 가격은 생성 시점 값 그대로 스냅샷된다`() {
        val item = newItem()

        assertEquals("짜장면", item.menuName)
        assertEquals(8000L, item.menuPrice)
    }
}
