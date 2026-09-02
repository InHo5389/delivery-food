package delivery.order.domain

import delivery.common.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrderTest {

    private fun newOrder(): Order = Order(
        customerId = 1L,
        shopId = 1L,
        menuId = 1L,
        menuName = "짜장면",
        menuPrice = 8000L,
        quantity = 2,
        customerName = "홍길동",
        customerPhone = "01011112222",
    )

    @Test
    fun `신규 생성 시 id는 null이다`() {
        val order = newOrder()

        assertNull(order.id)
    }

    @Test
    fun `신규 생성 시 기본 상태는 CREATED이다`() {
        val order = newOrder()

        assertEquals(OrderStatus.CREATED, order.status)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val order = Order.withId(
            id = 10L,
            customerId = 1L,
            shopId = 1L,
            menuId = 1L,
            menuName = "짜장면",
            menuPrice = 8000L,
            quantity = 2,
            customerName = "홍길동",
            customerPhone = "01011112222",
        )

        assertEquals(10L, order.id)
    }

    @Test
    fun `허용된 전이면 상태가 바뀐다`() {
        val order = newOrder()

        order.transitionTo(OrderStatus.PAID)

        assertEquals(OrderStatus.PAID, order.status)
    }

    @Test
    fun `허용되지 않은 전이는 예외가 발생하고 상태가 바뀌지 않는다`() {
        val order = newOrder()

        val exception = assertThrows<BusinessException> { order.transitionTo(OrderStatus.DELIVERED) }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
        assertEquals(OrderStatus.CREATED, order.status)
    }

    @Test
    fun `메뉴명과 가격은 생성 시점 값 그대로 스냅샷된다`() {
        val order = newOrder()

        assertEquals("짜장면", order.menuName)
        assertEquals(8000L, order.menuPrice)
    }

    @Test
    fun `고객 이름과 전화번호는 생성 시점 값 그대로 스냅샷된다`() {
        val order = newOrder()

        assertEquals("홍길동", order.customerName)
        assertEquals("01011112222", order.customerPhone)
    }
}
