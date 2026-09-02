package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.order.application.dto.AddCartItemCommand
import delivery.order.application.dto.ChangeCartItemQuantityCommand
import delivery.order.domain.Cart
import delivery.order.domain.CartErrorCode
import delivery.order.domain.CartItem
import delivery.order.infrastructure.CartItemRepository
import delivery.order.infrastructure.CartRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CartServiceTest {

    private val cartRepository = mockk<CartRepository>()
    private val cartItemRepository = mockk<CartItemRepository>()
    private lateinit var cartService: CartService

    @BeforeEach
    fun setUp() {
        cartService = CartService(cartRepository, cartItemRepository)
    }

    @Test
    fun `장바구니가 없으면 새로 생성하고 메뉴를 담는다`() {
        val command = AddCartItemCommand(customerId = 1L, shopId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 2)
        val cart = Cart.withId(1L, 1L, 1L)
        val newItem = CartItem.withId(1L, 1L, 1L, "짜장면", 8000L, 2)
        every { cartRepository.findByCustomerId(1L) } returns null andThen cart
        every { cartRepository.save(any()) } returns cart
        every { cartItemRepository.findByCartIdAndMenuId(1L, 1L) } returns null
        every { cartItemRepository.save(any()) } returns newItem
        every { cartItemRepository.findAllByCartId(1L) } returns listOf(newItem)

        val actual = cartService.addItem(command)

        assertEquals(1, actual.items.size)
        assertEquals("짜장면", actual.items[0].menuName)
    }

    @Test
    fun `이미 장바구니가 있으면 재사용한다`() {
        val command = AddCartItemCommand(customerId = 1L, shopId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1)
        val cart = Cart.withId(1L, 1L, 1L)
        every { cartRepository.findByCustomerId(1L) } returns cart
        every { cartItemRepository.findByCartIdAndMenuId(1L, 1L) } returns null
        every { cartItemRepository.save(any()) } answers { it.invocation.args[0] as CartItem }
        every { cartItemRepository.findAllByCartId(1L) } returns emptyList()

        cartService.addItem(command)

        verify(exactly = 0) { cartRepository.save(any()) }
    }

    @Test
    fun `이미 담긴 메뉴를 다시 담으면 수량이 합산된다`() {
        val command = AddCartItemCommand(customerId = 1L, shopId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 2)
        val cart = Cart.withId(1L, 1L, 1L)
        val existing = CartItem.withId(1L, 1L, 1L, "짜장면", 8000L, 1)
        every { cartRepository.findByCustomerId(1L) } returns cart
        every { cartItemRepository.findByCartIdAndMenuId(1L, 1L) } returns existing
        every { cartItemRepository.findAllByCartId(1L) } returns listOf(existing)

        cartService.addItem(command)

        assertEquals(3, existing.quantity)
    }

    @Test
    fun `다른 상점의 메뉴를 담으면 예외가 발생한다`() {
        val command = AddCartItemCommand(customerId = 1L, shopId = 2L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1)
        val cart = Cart.withId(1L, 1L, 1L)
        every { cartRepository.findByCustomerId(1L) } returns cart

        val exception = assertThrows<BusinessException> { cartService.addItem(command) }

        assertEquals(CartErrorCode.DIFFERENT_SHOP_IN_CART, exception.errorCode)
    }

    @Test
    fun `수량이 0이면 예외가 발생한다`() {
        val command = AddCartItemCommand(customerId = 1L, shopId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 0)

        val exception = assertThrows<BusinessException> { cartService.addItem(command) }

        assertEquals(CartErrorCode.INVALID_CART_ITEM_QUANTITY, exception.errorCode)
    }

    @Test
    fun `수량이 음수이면 예외가 발생한다`() {
        val command = AddCartItemCommand(customerId = 1L, shopId = 1L, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = -1)

        val exception = assertThrows<BusinessException> { cartService.addItem(command) }

        assertEquals(CartErrorCode.INVALID_CART_ITEM_QUANTITY, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 장바구니를 조회하면 예외가 발생한다`() {
        every { cartRepository.findByCustomerId(999L) } returns null

        val exception = assertThrows<BusinessException> { cartService.getCart(999L) }

        assertEquals(CartErrorCode.CART_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `장바구니를 조회하면 총액이 함께 계산된다`() {
        val cart = Cart.withId(1L, 1L, 1L)
        val items = listOf(
            CartItem.withId(1L, 1L, 1L, "짜장면", 8000L, 2),
            CartItem.withId(2L, 1L, 2L, "짬뽕", 9000L, 1),
        )
        every { cartRepository.findByCustomerId(1L) } returns cart
        every { cartItemRepository.findAllByCartId(1L) } returns items

        val actual = cartService.getCart(1L)

        assertEquals(25000L, actual.totalPrice)
    }

    @Test
    fun `항목 수량을 변경하면 반영된다`() {
        val cart = Cart.withId(1L, 1L, 1L)
        val item = CartItem.withId(1L, 1L, 1L, "짜장면", 8000L, 1)
        every { cartRepository.findByCustomerId(1L) } returns cart
        every { cartItemRepository.findAllByCartId(1L) } returns listOf(item)

        cartService.changeQuantity(ChangeCartItemQuantityCommand(customerId = 1L, cartItemId = 1L, quantity = 5))

        assertEquals(5, item.quantity)
    }

    @Test
    fun `존재하지 않는 항목의 수량을 변경하면 예외가 발생한다`() {
        val cart = Cart.withId(1L, 1L, 1L)
        every { cartRepository.findByCustomerId(1L) } returns cart
        every { cartItemRepository.findAllByCartId(1L) } returns emptyList()

        val exception = assertThrows<BusinessException> {
            cartService.changeQuantity(ChangeCartItemQuantityCommand(customerId = 1L, cartItemId = 999L, quantity = 1))
        }

        assertEquals(CartErrorCode.CART_ITEM_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `변경할 수량이 0이면 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> {
            cartService.changeQuantity(ChangeCartItemQuantityCommand(customerId = 1L, cartItemId = 1L, quantity = 0))
        }

        assertEquals(CartErrorCode.INVALID_CART_ITEM_QUANTITY, exception.errorCode)
    }

    @Test
    fun `항목을 삭제하면 저장소에서 제거된다`() {
        val cart = Cart.withId(1L, 1L, 1L)
        val item = CartItem.withId(1L, 1L, 1L, "짜장면", 8000L, 1)
        every { cartRepository.findByCustomerId(1L) } returns cart
        every { cartItemRepository.findAllByCartId(1L) } returns listOf(item)
        every { cartItemRepository.delete(item) } returns Unit

        cartService.removeItem(1L, 1L)

        verify { cartItemRepository.delete(item) }
    }

    @Test
    fun `존재하지 않는 항목을 삭제하면 예외가 발생한다`() {
        val cart = Cart.withId(1L, 1L, 1L)
        every { cartRepository.findByCustomerId(1L) } returns cart
        every { cartItemRepository.findAllByCartId(1L) } returns emptyList()

        val exception = assertThrows<BusinessException> { cartService.removeItem(1L, 999L) }

        assertEquals(CartErrorCode.CART_ITEM_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `장바구니를 비우면 모든 항목이 삭제된다`() {
        val cart = Cart.withId(1L, 1L, 1L)
        every { cartRepository.findByCustomerId(1L) } returns cart
        every { cartItemRepository.deleteAllByCartId(1L) } returns Unit

        cartService.clear(1L)

        verify { cartItemRepository.deleteAllByCartId(1L) }
    }
}
