package delivery.order.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.delivery.application.DeliveryService
import delivery.delivery.domain.Delivery
import delivery.order.application.dto.CartResult
import delivery.order.application.dto.CreateOrderCommand
import delivery.order.application.dto.OrderHistoryQuery
import delivery.order.domain.Cart
import delivery.order.domain.CartErrorCode
import delivery.order.domain.CartItem
import delivery.order.domain.Order
import delivery.order.domain.OrderErrorCode
import delivery.order.domain.OrderItem
import delivery.order.domain.OrderStatus
import delivery.order.domain.Payment
import delivery.order.domain.PaymentErrorCode
import delivery.order.domain.PaymentStatus
import delivery.order.infrastructure.OrderItemRepository
import delivery.order.infrastructure.OrderRepository
import delivery.shop.application.MenuService
import delivery.shop.application.OrderTicketService
import delivery.shop.application.ShopService
import delivery.shop.domain.Menu
import delivery.shop.domain.OrderTicket
import delivery.shop.domain.Shop
import delivery.shop.domain.ShopStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import kotlin.test.assertEquals

class OrderServiceTest {

    private val orderRepository = mockk<OrderRepository>()
    private val orderItemRepository = mockk<OrderItemRepository>()
    private val cartService = mockk<CartService>()
    private val paymentService = mockk<PaymentService>()
    private val shopService = mockk<ShopService>()
    private val menuService = mockk<MenuService>()
    private val orderTicketService = mockk<OrderTicketService>()
    private val deliveryService = mockk<DeliveryService>()
    private lateinit var orderService: OrderService

    private val customerId = 1L
    private val shopId = 1L
    private val menuId = 1L
    private val command = CreateOrderCommand(customerId, "홍길동", "01011112222")

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository, orderItemRepository, cartService, paymentService, shopService, menuService, orderTicketService, deliveryService)
    }

    private fun openShop(minOrderAmount: Long = 0): Shop =
        Shop.withId(shopId, 10L, "가게", "서울", "0212345678", status = ShopStatus.OPEN, minOrderAmount = minOrderAmount)

    private fun cartWith(quantity: Int = 1, price: Long = 8000L): CartResult {
        val cart = Cart.withId(1L, customerId, shopId)
        val item = CartItem.withId(1L, 1L, menuId, "짜장면", price, quantity)
        return CartResult(cart, listOf(item))
    }

    private fun stubOrderCreation(orderId: Long = 1L): Order {
        val savedOrder = Order.withId(orderId, customerId, shopId, "홍길동", "01011112222")
        every { orderRepository.save(any()) } returns savedOrder
        every { orderItemRepository.save(any()) } answers { it.invocation.args[0] as OrderItem }
        return savedOrder
    }

    @Test
    fun `장바구니에 담긴 항목이 없으면 예외가 발생한다`() {
        every { cartService.getCart(customerId) } returns CartResult(Cart.withId(1L, customerId, shopId), emptyList())

        val exception = assertThrows<BusinessException> { orderService.createOrder(command) }

        assertEquals(OrderErrorCode.EMPTY_CART, exception.errorCode)
    }

    @Test
    fun `장바구니 자체가 없으면 EMPTY_CART로 통일해서 예외가 발생한다`() {
        every { cartService.getCart(customerId) } throws BusinessException(CartErrorCode.CART_NOT_FOUND)

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
        stubOrderCreation()
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.APPROVED)
        every { cartService.clear(customerId) } returns Unit
        every { orderTicketService.createTicket(any()) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.createOrder(command)

        assertEquals(1, actual.items.size)
    }

    @Test
    fun `장바구니 항목이 여러 개면 주문 항목도 여러 개 생성된다`() {
        val cart = Cart.withId(1L, customerId, shopId)
        val items = listOf(
            CartItem.withId(1L, 1L, 1L, "짜장면", 8000L, 1),
            CartItem.withId(2L, 1L, 2L, "짬뽕", 9000L, 1),
        )
        every { cartService.getCart(customerId) } returns CartResult(cart, items)
        every { shopService.getById(shopId) } returns openShop()
        every { menuService.getMenuById(1L) } returns Menu.withId(1L, shopId, 1L, "짜장면", 8000L, 0)
        every { menuService.getMenuById(2L) } returns Menu.withId(2L, shopId, 1L, "짬뽕", 9000L, 1)
        stubOrderCreation()
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 17000L, PaymentStatus.APPROVED)
        every { cartService.clear(customerId) } returns Unit
        every { orderTicketService.createTicket(any()) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.createOrder(command)

        assertEquals(2, actual.items.size)
        assertEquals(17000L, actual.totalAmount)
    }

    @Test
    fun `결제가 승인되면 주문은 PAID 상태가 되고 장바구니가 비워진다`() {
        every { cartService.getCart(customerId) } returns cartWith()
        every { shopService.getById(shopId) } returns openShop()
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)
        val savedOrder = stubOrderCreation()
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.APPROVED)
        every { cartService.clear(customerId) } returns Unit
        every { orderTicketService.createTicket(any()) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.createOrder(command)

        assertEquals(OrderStatus.PAID, actual.order.status)
        assertEquals(savedOrder.id, actual.order.id)
        verify { cartService.clear(customerId) }
    }

    @Test
    fun `결제가 거절되면 주문은 PAYMENT_FAILED 상태가 되고 장바구니는 유지된다`() {
        every { cartService.getCart(customerId) } returns cartWith()
        every { shopService.getById(shopId) } returns openShop()
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)
        stubOrderCreation()
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.FAILED)

        val actual = orderService.createOrder(command)

        assertEquals(OrderStatus.PAYMENT_FAILED, actual.order.status)
        verify(exactly = 0) { cartService.clear(any()) }
    }

    @Test
    fun `내 주문을 조회하면 반환된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222")
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val actual = orderService.getOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))

        assertEquals(1L, actual.id)
    }

    @Test
    fun `다른 사람의 주문을 조회하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222")
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

    @Test
    fun `주문 내역을 페이지 단위로 조회한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222")
        val pageResult = PageImpl(listOf(order), PageRequest.of(0, 20), 1)
        every { orderRepository.findAllByCustomerIdOrderByIdDesc(customerId, PageRequest.of(0, 20)) } returns pageResult
        every { orderItemRepository.findAllByOrderId(1L) } returns listOf(
            OrderItem.withId(1L, 1L, menuId, "짜장면", 8000L, 1)
        )

        val actual = orderService.getMyOrderHistory(OrderHistoryQuery(customerId, page = 0, size = 20))

        assertEquals(1, actual.orders.size)
        assertEquals(1, actual.orders[0].items.size)
        assertEquals(1, actual.totalElements.toInt())
        assertEquals(1, actual.totalPages)
    }

    @Test
    fun `주문이 없으면 빈 목록을 반환한다`() {
        val pageResult = PageImpl<Order>(emptyList(), PageRequest.of(0, 20), 0)
        every { orderRepository.findAllByCustomerIdOrderByIdDesc(customerId, PageRequest.of(0, 20)) } returns pageResult

        val actual = orderService.getMyOrderHistory(OrderHistoryQuery(customerId, page = 0, size = 20))

        assertEquals(0, actual.orders.size)
        assertEquals(0, actual.totalElements.toInt())
    }

    @Test
    fun `두 번째 페이지를 요청하면 offset이 반영된다`() {
        val order = Order.withId(3L, customerId, shopId, "홍길동", "01011112222")
        val pageResult = PageImpl(listOf(order), PageRequest.of(1, 2), 3)
        every { orderRepository.findAllByCustomerIdOrderByIdDesc(customerId, PageRequest.of(1, 2)) } returns pageResult
        every { orderItemRepository.findAllByOrderId(3L) } returns emptyList()

        val actual = orderService.getMyOrderHistory(OrderHistoryQuery(customerId, page = 1, size = 2))

        assertEquals(1, actual.page)
        assertEquals(2, actual.totalPages)
    }

    @Test
    fun `CREATED 상태의 주문을 취소하면 환불을 시도하지 않는다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.CREATED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val actual = orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))

        assertEquals(OrderStatus.CANCELLED, actual.status)
        verify(exactly = 0) { paymentService.refund(any()) }
    }

    @Test
    fun `PAID 상태의 주문을 취소하면 환불을 시도한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { paymentService.refund(1L) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.REFUNDED)

        val actual = orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))

        assertEquals(OrderStatus.CANCELLED, actual.status)
        verify { paymentService.refund(1L) }
    }

    @Test
    fun `ACCEPTED 이후 상태의 주문을 취소하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.ACCEPTED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))
        }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
        verify(exactly = 0) { paymentService.refund(any()) }
    }

    @Test
    fun `이미 취소된 주문을 다시 취소하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.CANCELLED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))
        }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `다른 사람의 주문을 취소하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, AuthenticatedUser(999L, Role.CUSTOMER))
        }

        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `환불이 실패하면 예외가 발생한다 (실제 DB 반영은 @Transactional 롤백으로 방지됨)`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { paymentService.refund(1L) } throws BusinessException(PaymentErrorCode.REFUND_FAILED)

        val exception = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))
        }

        assertEquals(PaymentErrorCode.REFUND_FAILED, exception.errorCode)
    }

    private val ownerId = 10L

    @Test
    fun `사장님이 PAID 주문을 수락하면 ACCEPTED가 되고 티켓·배차 요청이 함께 생성된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { orderTicketService.markAccepted(1L) } returns OrderTicket.withId(1L, 1L, shopId)
        every { deliveryService.createDelivery(any()) } returns Delivery.withId(1L, orderId = 1L, shopId = shopId)

        val actual = orderService.acceptOrder(1L, AuthenticatedUser(ownerId, Role.OWNER), estimatedCookingMinutes = 15)

        assertEquals(OrderStatus.ACCEPTED, actual.status)
        verify { orderTicketService.markAccepted(1L) }
        verify { deliveryService.createDelivery(any()) }
    }

    @Test
    fun `사장님이 아닌 사용자가 수락하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.acceptOrder(1L, AuthenticatedUser(999L, Role.OWNER), estimatedCookingMinutes = 15)
        }

        assertEquals(OrderErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `다른 상점의 사장님이 수락하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.acceptOrder(1L, AuthenticatedUser(ownerId, Role.CUSTOMER), estimatedCookingMinutes = 15)
        }

        assertEquals(OrderErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `CREATED 상태의 주문을 수락하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.CREATED)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.acceptOrder(1L, AuthenticatedUser(ownerId, Role.OWNER), estimatedCookingMinutes = 15)
        }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `사장님이 PAID 주문을 거절하면 REJECTED가 되고 환불된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { orderTicketService.markRejected(1L) } returns OrderTicket.withId(1L, 1L, shopId)
        every { paymentService.refund(1L) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.REFUNDED)

        val actual = orderService.rejectOrder(1L, AuthenticatedUser(ownerId, Role.OWNER))

        assertEquals(OrderStatus.REJECTED, actual.status)
        verify { paymentService.refund(1L) }
    }

    @Test
    fun `사장님이 ACCEPTED 주문을 조리 시작하면 COOKING이 된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.ACCEPTED)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { orderTicketService.markCookingStarted(1L) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.startCooking(1L, AuthenticatedUser(ownerId, Role.OWNER))

        assertEquals(OrderStatus.COOKING, actual.status)
    }

    @Test
    fun `ACCEPTED 이전 주문을 조리 시작하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.startCooking(1L, AuthenticatedUser(ownerId, Role.OWNER))
        }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `사장님이 COOKING 주문을 조리 완료 처리하면 COOKED가 된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", OrderStatus.COOKING)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { orderTicketService.markCookingDone(1L) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.finishCooking(1L, AuthenticatedUser(ownerId, Role.OWNER))

        assertEquals(OrderStatus.COOKED, actual.status)
    }
}
