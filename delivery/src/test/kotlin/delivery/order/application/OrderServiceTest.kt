package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.auth.domain.Role
import delivery.order.application.dto.CartResult
import delivery.order.application.dto.CreateOrderCommand
import delivery.order.domain.Cart
import delivery.order.domain.CartItem
import delivery.order.domain.Order
import delivery.order.domain.OrderErrorCode
import delivery.order.domain.OrderStatus
import delivery.order.domain.Payment
import delivery.order.domain.PaymentStatus
import delivery.order.infrastructure.OrderRepository
import delivery.shop.application.MenuService
import delivery.shop.application.ShopService
import delivery.shop.domain.Menu
import delivery.shop.domain.Shop
import delivery.shop.domain.ShopStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals

class OrderServiceTest {

    private val orderRepository = mockk<OrderRepository>()
    private val cartService = mockk<CartService>()
    private val paymentService = mockk<PaymentService>()
    private val shopService = mockk<ShopService>()
    private val menuService = mockk<MenuService>()
    private lateinit var orderService: OrderService

    private val customerId = 1L
    private val shopId = 1L
    private val menuId = 1L
    private val command = CreateOrderCommand(customerId, "홍길동", "01011112222")

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository, cartService, paymentService, shopService, menuService)
    }

    private fun openShop(minOrderAmount: Long = 0): Shop =
        Shop.withId(shopId, 10L, "가게", "서울", "0212345678", status = ShopStatus.OPEN, minOrderAmount = minOrderAmount)

    private fun cartWith(quantity: Int = 1, price: Long = 8000L): CartResult {
        val cart = Cart.withId(1L, customerId, shopId)
        val item = CartItem.withId(1L, 1L, menuId, "짜장면", price, quantity)
        return CartResult(cart, listOf(item))
    }

    @Test
    fun `장바구니에 담긴 항목이 없으면 예외가 발생한다`() {
        every { cartService.getCart(customerId) } returns CartResult(Cart.withId(1L, customerId, shopId), emptyList())

        val exception = assertThrows<BusinessException> { orderService.createOrder(command) }

        assertEquals(OrderErrorCode.EMPTY_CART, exception.errorCode)
    }

    @Test
    fun `장바구니 자체가 없으면 EMPTY_CART로 통일해서 예외가 발생한다`() {
        every { cartService.getCart(customerId) } throws BusinessException(delivery.order.domain.CartErrorCode.CART_NOT_FOUND)

        val exception = assertThrows<BusinessException> { orderService.createOrder(command) }

        assertEquals(OrderErrorCode.EMPTY_CART, exception.errorCode)
    }

    @Test
    fun `상점이 영업중이 아니면 예외가 발생한다`() {
        every { cartService.getCart(customerId) } returns cartWith()
        every { shopService.getById(shopId) } returns Shop.withId(shopId, 10L, "가게", "서울", "0212345678", status = ShopStatus.CLOSED)

        val exception = assertThrows<BusinessException> { orderService.createOrder(command) }

        assertEquals(OrderErrorCode.SHOP_NOT_OPEN, exception.errorCode)
    }

    @Test
    fun `메뉴가 품절이면 예외가 발생한다`() {
        every { cartService.getCart(customerId) } returns cartWith()
        every { shopService.getById(shopId) } returns openShop()
        val menu = Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0).apply { soldOut = true }
        every { menuService.getMenuById(menuId) } returns menu

        val exception = assertThrows<BusinessException> { orderService.createOrder(command) }

        assertEquals(OrderErrorCode.MENU_SOLD_OUT, exception.errorCode)
    }

    @Test
    fun `장바구니에 담긴 가격과 현재 메뉴 가격이 다르면 예외가 발생한다`() {
        every { cartService.getCart(customerId) } returns cartWith(price = 8000L)
        every { shopService.getById(shopId) } returns openShop()
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 9000L, 0)

        val exception = assertThrows<BusinessException> { orderService.createOrder(command) }

        assertEquals(OrderErrorCode.MENU_PRICE_CHANGED, exception.errorCode)
    }

    @Test
    fun `최소주문금액 미만이면 예외가 발생한다`() {
        every { cartService.getCart(customerId) } returns cartWith(quantity = 1, price = 8000L)
        every { shopService.getById(shopId) } returns openShop(minOrderAmount = 10000L)
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)

        val exception = assertThrows<BusinessException> { orderService.createOrder(command) }

        assertEquals(OrderErrorCode.BELOW_MIN_ORDER_AMOUNT, exception.errorCode)
    }

    @Test
    fun `최소주문금액과 정확히 같으면 주문이 생성된다`() {
        every { cartService.getCart(customerId) } returns cartWith(quantity = 1, price = 8000L)
        every { shopService.getById(shopId) } returns openShop(minOrderAmount = 8000L)
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)
        val savedOrder = Order.withId(1L, customerId, shopId, menuId, "짜장면", 8000L, 1, "홍길동", "01011112222")
        every { orderRepository.save(any()) } returns savedOrder
        val approvedPayment = Payment.withId(1L, 1L, 8000L, PaymentStatus.APPROVED)
        every { paymentService.requestPayment(any()) } returns approvedPayment
        every { cartService.clear(customerId) } returns Unit

        val actual = orderService.createOrder(command)

        assertEquals(1, actual.orders.size)
    }

    @Test
    fun `결제가 승인되면 주문은 PAID 상태가 되고 장바구니가 비워진다`() {
        every { cartService.getCart(customerId) } returns cartWith()
        every { shopService.getById(shopId) } returns openShop()
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)
        val savedOrder = Order.withId(1L, customerId, shopId, menuId, "짜장면", 8000L, 1, "홍길동", "01011112222")
        every { orderRepository.save(any()) } returns savedOrder
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.APPROVED)
        every { cartService.clear(customerId) } returns Unit

        val actual = orderService.createOrder(command)

        assertEquals(OrderStatus.PAID, actual.orders.first().status)
        verify { cartService.clear(customerId) }
    }

    @Test
    fun `결제가 거절되면 주문은 PAYMENT_FAILED 상태가 되고 장바구니는 유지된다`() {
        every { cartService.getCart(customerId) } returns cartWith()
        every { shopService.getById(shopId) } returns openShop()
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)
        val savedOrder = Order.withId(1L, customerId, shopId, menuId, "짜장면", 8000L, 1, "홍길동", "01011112222")
        every { orderRepository.save(any()) } returns savedOrder
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.FAILED)

        val actual = orderService.createOrder(command)

        assertEquals(OrderStatus.PAYMENT_FAILED, actual.orders.first().status)
        verify(exactly = 0) { cartService.clear(any()) }
    }

    @Test
    fun `내 주문을 조회하면 반환된다`() {
        val order = Order.withId(1L, customerId, shopId, menuId, "짜장면", 8000L, 1, "홍길동", "01011112222")
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val actual = orderService.getOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))

        assertEquals(1L, actual.id)
    }

    @Test
    fun `다른 사람의 주문을 조회하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, menuId, "짜장면", 8000L, 1, "홍길동", "01011112222")
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.getOrder(1L, AuthenticatedUser(999L, Role.CUSTOMER))
        }

        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 주문을 조회하면 예외가 발생한다`() {
        every { orderRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> {
            orderService.getOrder(999L, AuthenticatedUser(customerId, Role.CUSTOMER))
        }

        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, exception.errorCode)
    }
}
