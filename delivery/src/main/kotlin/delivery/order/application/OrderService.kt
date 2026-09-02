package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.order.application.dto.CreateOrderCommand
import delivery.order.application.dto.OrderHistoryItem
import delivery.order.application.dto.OrderHistoryQuery
import delivery.order.application.dto.OrderHistoryResult
import delivery.order.application.dto.OrderResult
import delivery.order.application.dto.RequestPaymentCommand
import delivery.order.domain.CartErrorCode
import delivery.order.domain.Order
import delivery.order.domain.OrderErrorCode
import delivery.order.domain.OrderItem
import delivery.order.domain.OrderStatus
import delivery.order.domain.PaymentStatus
import delivery.order.infrastructure.OrderItemRepository
import delivery.order.infrastructure.OrderRepository
import delivery.shop.application.MenuService
import delivery.shop.application.ShopService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// ★ 모듈 간 호출은 shopService/menuService를 직접 주입해서 쓴다(Facade를 두지 않음).
//   지금은 모놀리스라 이벤트를 쓰지 않는다 — ApplicationEvent를 쓰면 "이벤트를 썼다"는
//   형식만 남고 비동기·장애격리 같은 실제 이득은 없다. Phase 5(MSA)에서 이 직접 호출이
//   REST/Kafka로 바뀌는 지점을 측정해 "동기 호출이 전체 응답의 몇 %"를 보여주는 것이
//   이 설계의 목적이다.
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val cartService: CartService,
    private val paymentService: PaymentService,
    private val shopService: ShopService,
    private val menuService: MenuService,
) {
    @Transactional
    fun createOrder(command: CreateOrderCommand): OrderResult {
        // 장바구니가 아예 없는 경우(한 번도 담은 적 없음)와 담긴 항목이 0개인 경우는
        // 고객 입장에서 "주문할 게 없다"는 같은 의미다. CartService는 전자를 CART_NOT_FOUND로
        // 구분하지만, 주문 생성 시점에는 둘 다 EMPTY_CART로 통일해서 응답한다.
        val cart = try {
            cartService.getCart(command.customerId)
        } catch (e: BusinessException) {
            if (e.errorCode == CartErrorCode.CART_NOT_FOUND) throw BusinessException(OrderErrorCode.EMPTY_CART) else throw e
        }
        if (cart.items.isEmpty()) {
            throw BusinessException(OrderErrorCode.EMPTY_CART)
        }

        val shop = shopService.getById(cart.cart.shopId)
        if (!shop.isOpen()) {
            throw BusinessException(OrderErrorCode.SHOP_NOT_OPEN)
        }

        // 주문 시점 재검증 — 장바구니에 담은 이후 사장님이 품절 처리하거나
        // 가격을 바꿨을 수 있으므로 담을 때가 아니라 지금 다시 확인한다.
        cart.items.forEach { item ->
            val menu = menuService.getMenuById(item.menuId)
            if (menu.soldOut) {
                throw BusinessException(OrderErrorCode.MENU_SOLD_OUT)
            }
            if (menu.price != item.menuPrice) {
                throw BusinessException(OrderErrorCode.MENU_PRICE_CHANGED)
            }
        }

        if (cart.totalPrice < shop.minOrderAmount) {
            throw BusinessException(OrderErrorCode.BELOW_MIN_ORDER_AMOUNT)
        }

        val order = orderRepository.save(
            Order(
                customerId = command.customerId,
                shopId = cart.cart.shopId,
                customerName = command.customerName,
                customerPhone = command.customerPhone,
            )
        )

        // ⚠️ 의도적 구식 구현 — Phase 3 B-6에서 JDBC batch insert로 개선 예정.
        //   장바구니 항목 수만큼 INSERT가 건별로 나간다.
        val items = cart.items.map { item ->
            orderItemRepository.save(
                OrderItem(
                    orderId = order.id!!,
                    menuId = item.menuId,
                    menuName = item.menuName,
                    menuPrice = item.menuPrice,
                    quantity = item.quantity,
                )
            )
        }

        val payment = paymentService.requestPayment(
            RequestPaymentCommand(orderId = order.id!!, amount = cart.totalPrice)
        )

        val nextStatus = if (payment.status == PaymentStatus.APPROVED) OrderStatus.PAID else OrderStatus.PAYMENT_FAILED
        order.transitionTo(nextStatus)

        if (nextStatus == OrderStatus.PAID) {
            cartService.clear(command.customerId)
        }

        return OrderResult(order, items, payment)
    }

    // ⚠️ 의도적 구식 구현 — Phase 3 A-3에서 커서 기반 페이징으로 개선 예정.
    //   OFFSET 페이징은 페이지가 깊어질수록 앞의 행을 읽고 버려 선형적으로 느려진다.
    // ⚠️ 의도적 구식 구현 — Phase 3 A-4에서 fetch join/batch fetch로 개선 예정.
    //   주문 목록의 각 주문마다 항목을 별도로 조회하면 N+1이 발생한다.
    fun getMyOrderHistory(query: OrderHistoryQuery): OrderHistoryResult {
        val page = orderRepository.findAllByCustomerIdOrderByIdDesc(
            query.customerId,
            PageRequest.of(query.page, query.size),
        )
        val historyItems = page.content.map { order ->
            OrderHistoryItem(order, orderItemRepository.findAllByOrderId(order.id!!))
        }
        return OrderHistoryResult(
            orders = historyItems,
            page = query.page,
            size = query.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    fun getOrder(orderId: Long, requester: AuthenticatedUser): Order {
        val order = orderRepository.findById(orderId).orElseThrow { BusinessException(OrderErrorCode.ORDER_NOT_FOUND) }
        if (order.customerId != requester.userId) {
            throw BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
        }
        return order
    }

    fun getOrderItems(orderId: Long): List<OrderItem> = orderItemRepository.findAllByOrderId(orderId)

    // ACCEPTED 이전(CREATED/PAID)까지만 자유 취소 가능 — 상태머신(OrderStatus)이
    // 이미 이 규칙을 강제하므로 여기서는 별도 시점 검증 없이 transitionTo에 위임한다.
    // ACCEPTED 이후 취소를 시도하면 INVALID_ORDER_STATUS_TRANSITION이 그대로 올라간다.
    @Transactional
    fun cancelOrder(orderId: Long, requester: AuthenticatedUser): Order {
        val order = getOrder(orderId, requester)
        val wasPaid = order.status == OrderStatus.PAID

        order.transitionTo(OrderStatus.CANCELLED)

        if (wasPaid) {
            paymentService.refund(order.id!!)
        }

        return order
    }
}
