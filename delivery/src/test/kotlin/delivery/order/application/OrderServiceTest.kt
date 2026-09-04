package delivery.order.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.delivery.application.DeliveryService
import delivery.delivery.domain.Delivery
import delivery.notification.application.NotificationService
import delivery.order.application.dto.CartResult
import delivery.order.application.dto.CreateOrderCommand
import delivery.order.application.dto.OrderHistoryQuery
import delivery.order.application.dto.SalesSummaryQuery
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
import delivery.order.infrastructure.SalesSummaryRepository
import delivery.order.infrastructure.SalesSummaryRow
import delivery.order.infrastructure.ShopSettlementSourceRepository
import delivery.shop.application.MenuService
import delivery.shop.application.OrderTicketService
import delivery.shop.application.ShopService
import delivery.shop.domain.Menu
import delivery.shop.domain.OrderTicket
import delivery.shop.domain.Shop
import delivery.shop.domain.ShopStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    private val salesSummaryRepository = mockk<SalesSummaryRepository>()
    private val shopSettlementSourceRepository = mockk<ShopSettlementSourceRepository>()
    private val notificationService = mockk<NotificationService>(relaxed = true)
    private lateinit var orderService: OrderService

    private val customerId = 1L
    private val shopId = 1L
    private val menuId = 1L
    private val command = CreateOrderCommand(customerId, "홍길동", "01011112222")

    @BeforeEach
    fun setUp() {
        orderService = OrderService(
            orderRepository, orderItemRepository, cartService, paymentService, shopService, menuService,
            orderTicketService, deliveryService, salesSummaryRepository, shopSettlementSourceRepository,
            notificationService,
        )
    }

    private fun openShop(minOrderAmount: Long = 0, deliveryFee: Long = 0): Shop =
        Shop.withId(shopId, 10L, "가게", "서울", "0212345678", status = ShopStatus.OPEN, minOrderAmount = minOrderAmount, deliveryFee = deliveryFee)

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
    fun `배달비가 있는 상점에서 주문하면 결제 금액에 메뉴 합계와 배달비가 함께 청구된다`() {
        every { cartService.getCart(customerId) } returns cartWith(quantity = 1, price = 8000L)
        every { shopService.getById(shopId) } returns openShop(deliveryFee = 3000L)
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)
        stubOrderCreation()
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 11000L, PaymentStatus.APPROVED)
        every { cartService.clear(customerId) } returns Unit
        every { orderTicketService.createTicket(any()) } returns OrderTicket.withId(1L, 1L, shopId)

        orderService.createOrder(command)

        verify { paymentService.requestPayment(withArg { assertEquals(11000L, it.amount) }) }
    }

    @Test
    fun `주문에는 상점의 배달비가 스냅샷으로 저장된다`() {
        every { cartService.getCart(customerId) } returns cartWith(quantity = 1, price = 8000L)
        every { shopService.getById(shopId) } returns openShop(deliveryFee = 3000L)
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)
        stubOrderCreation()
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 11000L, PaymentStatus.APPROVED)
        every { cartService.clear(customerId) } returns Unit
        every { orderTicketService.createTicket(any()) } returns OrderTicket.withId(1L, 1L, shopId)

        orderService.createOrder(command)

        verify { orderRepository.save(withArg { assertEquals(3000L, it.deliveryFee) }) }
    }

    @Test
    fun `배달비가 0원인 상점에서 주문하면 결제 금액은 메뉴 합계와 같다`() {
        every { cartService.getCart(customerId) } returns cartWith(quantity = 1, price = 8000L)
        every { shopService.getById(shopId) } returns openShop(deliveryFee = 0L)
        every { menuService.getMenuById(menuId) } returns Menu.withId(menuId, shopId, 1L, "짜장면", 8000L, 0)
        stubOrderCreation()
        every { paymentService.requestPayment(any()) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.APPROVED)
        every { cartService.clear(customerId) } returns Unit
        every { orderTicketService.createTicket(any()) } returns OrderTicket.withId(1L, 1L, shopId)

        orderService.createOrder(command)

        verify { paymentService.requestPayment(withArg { assertEquals(8000L, it.amount) }) }
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
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.CREATED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val actual = orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))

        assertEquals(OrderStatus.CANCELLED, actual.status)
        verify(exactly = 0) { paymentService.refund(any()) }
        verify { notificationService.notify(customerId, 1L, "주문이 취소되었습니다.") }
    }

    @Test
    fun `PAID 상태의 주문을 취소하면 환불을 시도하고 티켓도 CANCELLED로 정리된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { paymentService.refund(1L) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.REFUNDED)
        every { orderTicketService.markCancelled(1L) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))

        assertEquals(OrderStatus.CANCELLED, actual.status)
        verify { paymentService.refund(1L) }
        verify { orderTicketService.markCancelled(1L) }
    }

    @Test
    fun `ACCEPTED 이후 상태의 주문을 취소하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.ACCEPTED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))
        }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
        verify(exactly = 0) { paymentService.refund(any()) }
    }

    @Test
    fun `이미 취소된 주문을 다시 취소하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.CANCELLED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, AuthenticatedUser(customerId, Role.CUSTOMER))
        }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `다른 사람의 주문을 취소하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, AuthenticatedUser(999L, Role.CUSTOMER))
        }

        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `환불이 실패하면 예외가 발생한다 (실제 DB 반영은 @Transactional 롤백으로 방지됨)`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
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
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { orderTicketService.markAccepted(1L) } returns OrderTicket.withId(1L, 1L, shopId)
        every { deliveryService.createDelivery(any()) } returns Delivery.withId(1L, orderId = 1L, shopId = shopId)

        val actual = orderService.acceptOrder(1L, AuthenticatedUser(ownerId, Role.OWNER), estimatedCookingMinutes = 15)

        assertEquals(OrderStatus.ACCEPTED, actual.status)
        verify { orderTicketService.markAccepted(1L) }
        verify { deliveryService.createDelivery(any()) }
        verify { notificationService.notify(customerId, 1L, "주문이 접수되었습니다.") }
    }

    @Test
    fun `사장님이 아닌 사용자가 수락하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.acceptOrder(1L, AuthenticatedUser(999L, Role.OWNER), estimatedCookingMinutes = 15)
        }

        assertEquals(OrderErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `다른 상점의 사장님이 수락하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.acceptOrder(1L, AuthenticatedUser(ownerId, Role.CUSTOMER), estimatedCookingMinutes = 15)
        }

        assertEquals(OrderErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `CREATED 상태의 주문을 수락하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.CREATED)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.acceptOrder(1L, AuthenticatedUser(ownerId, Role.OWNER), estimatedCookingMinutes = 15)
        }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `사장님이 PAID 주문을 거절하면 REJECTED가 되고 환불된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { orderTicketService.markRejected(1L) } returns OrderTicket.withId(1L, 1L, shopId)
        every { paymentService.refund(1L) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.REFUNDED)

        val actual = orderService.rejectOrder(1L, AuthenticatedUser(ownerId, Role.OWNER))

        assertEquals(OrderStatus.REJECTED, actual.status)
        verify { paymentService.refund(1L) }
        verify { notificationService.notify(customerId, 1L, "주문이 거절되었습니다.") }
    }

    @Test
    fun `사장님이 ACCEPTED 주문을 조리 시작하면 COOKING이 된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.ACCEPTED)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { orderTicketService.markCookingStarted(1L) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.startCooking(1L, AuthenticatedUser(ownerId, Role.OWNER))

        assertEquals(OrderStatus.COOKING, actual.status)
        verify { notificationService.notify(customerId, 1L, "조리를 시작했습니다.") }
    }

    @Test
    fun `ACCEPTED 이전 주문을 조리 시작하려 하면 예외가 발생한다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.startCooking(1L, AuthenticatedUser(ownerId, Role.OWNER))
        }

        assertEquals(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `사장님이 COOKING 주문을 조리 완료 처리하면 COOKED가 된다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.COOKING)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { orderTicketService.markCookingDone(1L) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.finishCooking(1L, AuthenticatedUser(ownerId, Role.OWNER))

        assertEquals(OrderStatus.COOKED, actual.status)
        verify { notificationService.notify(customerId, 1L, "조리가 완료되었습니다.") }
    }

    @Test
    fun `여전히 PAID인 주문을 자동 취소하면 CANCELLED가 되고 환불·티켓 정리가 함께 일어난다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { paymentService.refund(1L) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.REFUNDED)
        every { orderTicketService.markCancelled(1L) } returns OrderTicket.withId(1L, 1L, shopId)

        orderService.autoCancelIfStale(1L)

        assertEquals(OrderStatus.CANCELLED, order.status)
        verify { paymentService.refund(1L) }
        verify { orderTicketService.markCancelled(1L) }
        verify { notificationService.notify(customerId, 1L, "주문이 취소되었습니다.") }
    }

    @Test
    fun `그 사이 이미 ACCEPTED된 주문은 자동 취소하지 않고 조용히 넘어간다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.ACCEPTED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        orderService.autoCancelIfStale(1L)

        assertEquals(OrderStatus.ACCEPTED, order.status)
        verify(exactly = 0) { paymentService.refund(any()) }
        verify(exactly = 0) { orderTicketService.markCancelled(any()) }
        verify(exactly = 0) { notificationService.notify(any(), any(), any()) }
    }

    @Test
    fun `라이더가 배정되면 고객에게 알림을 보낸다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.ACCEPTED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        orderService.markRiderAssigned(1L)

        assertEquals(OrderStatus.RIDER_ASSIGNED, order.status)
        verify { notificationService.notify(customerId, 1L, "라이더가 배정되었습니다.") }
    }

    @Test
    fun `존재하지 않는 주문에 라이더를 배정하려 하면 예외가 발생한다`() {
        every { orderRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { orderService.markRiderAssigned(999L) }

        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `픽업되면 고객에게 알림을 보낸다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.COOKED)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        orderService.markPickedUp(1L)

        assertEquals(OrderStatus.PICKED_UP, order.status)
        verify { notificationService.notify(customerId, 1L, "라이더가 음식을 픽업했습니다.") }
    }

    @Test
    fun `배달완료되면 고객에게 알림을 보낸다`() {
        val order = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PICKED_UP)
        every { orderRepository.findById(1L) } returns Optional.of(order)

        orderService.markDelivered(1L)

        assertEquals(OrderStatus.DELIVERED, order.status)
        verify { notificationService.notify(customerId, 1L, "배달이 완료되었습니다.") }
    }

    @Test
    fun `존재하지 않는 주문을 자동 취소하려 하면 예외가 발생한다`() {
        every { orderRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { orderService.autoCancelIfStale(999L) }

        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `threshold보다 오래 PAID로 머문 주문들을 한 번에 자동 취소한다`() {
        val stale1 = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        val stale2 = Order.withId(2L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findAllByStatusAndUpdatedAtBefore(OrderStatus.PAID, any()) } returns listOf(stale1, stale2)
        every { orderRepository.findById(1L) } returns Optional.of(stale1)
        every { orderRepository.findById(2L) } returns Optional.of(stale2)
        every { paymentService.refund(any()) } returns Payment.withId(1L, 1L, 8000L, PaymentStatus.REFUNDED)
        every { orderTicketService.markCancelled(any()) } returns OrderTicket.withId(1L, 1L, shopId)

        val actual = orderService.autoCancelStaleOrders()

        assertEquals(listOf(1L, 2L), actual)
        assertEquals(OrderStatus.CANCELLED, stale1.status)
        assertEquals(OrderStatus.CANCELLED, stale2.status)
    }

    @Test
    fun `한 건이 실패해도 나머지 자동 취소는 계속 진행된다`() {
        val failing = Order.withId(1L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        val succeeding = Order.withId(2L, customerId, shopId, "홍길동", "01011112222", status = OrderStatus.PAID)
        every { orderRepository.findAllByStatusAndUpdatedAtBefore(OrderStatus.PAID, any()) } returns listOf(failing, succeeding)
        every { orderRepository.findById(1L) } throws RuntimeException("DB 커넥션 오류")
        every { orderRepository.findById(2L) } returns Optional.of(succeeding)
        every { paymentService.refund(2L) } returns Payment.withId(1L, 2L, 8000L, PaymentStatus.REFUNDED)
        every { orderTicketService.markCancelled(2L) } returns OrderTicket.withId(1L, 2L, shopId)

        val actual = orderService.autoCancelStaleOrders()

        assertEquals(listOf(2L), actual)
    }

    @Test
    fun `사장님이 매출을 조회하면 DELIVERED 기준 집계를 그대로 반환한다`() {
        val date = LocalDate.of(2026, 9, 3)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { salesSummaryRepository.findSales(shopId, any(), any()) } returns SalesSummaryRow(orderCount = 3L, totalAmount = 45000L)

        val actual = orderService.getSalesSummary(SalesSummaryQuery(shopId, date), AuthenticatedUser(ownerId, Role.OWNER))

        assertEquals(date, actual.date)
        assertEquals(3L, actual.orderCount)
        assertEquals(45000L, actual.totalAmount)
    }

    @Test
    fun `매출 조회 시 KST 하루 경계로 조회 범위를 계산한다`() {
        val date = LocalDate.of(2026, 9, 3)
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        val fromSlot = slot<Instant>()
        val toSlot = slot<Instant>()
        every { salesSummaryRepository.findSales(shopId, capture(fromSlot), capture(toSlot)) } returns SalesSummaryRow(0L, 0L)

        orderService.getSalesSummary(SalesSummaryQuery(shopId, date), AuthenticatedUser(ownerId, Role.OWNER))

        val zone = ZoneId.of("Asia/Seoul")
        assertEquals(date.atStartOfDay(zone).toInstant(), fromSlot.captured)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), toSlot.captured)
    }

    @Test
    fun `사장님이 아니면 매출을 조회할 수 없다`() {
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            orderService.getSalesSummary(SalesSummaryQuery(shopId, LocalDate.of(2026, 9, 3)), AuthenticatedUser(999L, Role.OWNER))
        }

        assertEquals(OrderErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `주문 ID로 배달비 스냅샷을 조회하면 주문별 배달비를 반환한다`() {
        every { orderRepository.findAllById(listOf(1L, 2L)) } returns listOf(
            Order.withId(1L, customerId, shopId, "홍길동", "01011112222", deliveryFee = 3000L),
            Order.withId(2L, customerId, shopId, "홍길동", "01011112222", deliveryFee = 0L),
        )

        val actual = orderService.getDeliveryFees(listOf(1L, 2L))

        assertEquals(3000L, actual[1L])
        assertEquals(0L, actual[2L])
    }

    @Test
    fun `빈 주문 ID 목록으로 배달비를 조회하면 빈 맵을 반환한다`() {
        every { orderRepository.findAllById(emptyList()) } returns emptyList()

        val actual = orderService.getDeliveryFees(emptyList())

        assertEquals(emptyMap(), actual)
    }
}
