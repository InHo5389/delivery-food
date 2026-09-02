package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.order.application.dto.AddCartItemCommand
import delivery.order.application.dto.ChangeCartItemQuantityCommand
import delivery.order.domain.CartErrorCode
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class CartServiceIntegrationTest(
    @Autowired private val cartService: CartService,
) : IntegrationTestSupport() {

    @Test
    fun `장바구니에 메뉴를 담고 조회하면 저장된 항목이 반환된다`() {
        val customerId = System.nanoTime()
        val command = AddCartItemCommand(customerId = customerId, shopId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 2)

        cartService.addItem(command)
        val actual = cartService.getCart(customerId)

        assertEquals(1, actual.items.size)
        assertEquals("짜장면", actual.items[0].menuName)
        assertEquals(2, actual.items[0].quantity)
        assertEquals(16000L, actual.totalPrice)
    }

    @Test
    fun `같은 메뉴를 두 번 담으면 하나의 항목으로 수량이 합산된다`() {
        val customerId = System.nanoTime()
        cartService.addItem(AddCartItemCommand(customerId, 1L, 1L, "짜장면", 8000L, 1))

        cartService.addItem(AddCartItemCommand(customerId, 1L, 1L, "짜장면", 8000L, 2))
        val actual = cartService.getCart(customerId)

        assertEquals(1, actual.items.size)
        assertEquals(3, actual.items[0].quantity)
    }

    @Test
    fun `다른 상점의 메뉴를 담으려 하면 예외가 발생하고 기존 장바구니는 유지된다`() {
        val customerId = System.nanoTime()
        cartService.addItem(AddCartItemCommand(customerId, 1L, 1L, "짜장면", 8000L, 1))

        val exception = assertThrows<BusinessException> {
            cartService.addItem(AddCartItemCommand(customerId, 2L, 2L, "탕수육", 18000L, 1))
        }

        assertEquals(CartErrorCode.DIFFERENT_SHOP_IN_CART, exception.errorCode)
        assertEquals(1, cartService.getCart(customerId).items.size)
    }

    @Test
    fun `항목 수량을 변경하면 총액에 반영된다`() {
        val customerId = System.nanoTime()
        cartService.addItem(AddCartItemCommand(customerId, 1L, 1L, "짜장면", 8000L, 1))
        val itemId = cartService.getCart(customerId).items[0].id!!

        cartService.changeQuantity(ChangeCartItemQuantityCommand(customerId, itemId, 5))
        val actual = cartService.getCart(customerId)

        assertEquals(5, actual.items[0].quantity)
        assertEquals(40000L, actual.totalPrice)
    }

    @Test
    fun `항목을 삭제하면 장바구니에서 사라진다`() {
        val customerId = System.nanoTime()
        cartService.addItem(AddCartItemCommand(customerId, 1L, 1L, "짜장면", 8000L, 1))
        val itemId = cartService.getCart(customerId).items[0].id!!

        cartService.removeItem(customerId, itemId)
        val actual = cartService.getCart(customerId)

        assertEquals(0, actual.items.size)
    }

    @Test
    fun `장바구니를 비우면 모든 항목이 사라지지만 장바구니 자체는 남는다`() {
        val customerId = System.nanoTime()
        cartService.addItem(AddCartItemCommand(customerId, 1L, 1L, "짜장면", 8000L, 1))
        cartService.addItem(AddCartItemCommand(customerId, 1L, 2L, "짬뽕", 9000L, 1))

        cartService.clear(customerId)
        val actual = cartService.getCart(customerId)

        assertEquals(0, actual.items.size)
    }

    @Test
    fun `존재하지 않는 고객의 장바구니를 조회하면 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { cartService.getCart(Long.MAX_VALUE) }

        assertEquals(CartErrorCode.CART_NOT_FOUND, exception.errorCode)
    }
}
