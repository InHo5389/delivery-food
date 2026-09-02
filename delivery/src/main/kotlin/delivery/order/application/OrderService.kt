package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.order.application.dto.CreateOrderCommand
import delivery.order.application.dto.OrderResult
import delivery.order.application.dto.RequestPaymentCommand
import delivery.order.domain.CartErrorCode
import delivery.order.domain.Order
import delivery.order.domain.OrderErrorCode
import delivery.order.domain.OrderStatus
import delivery.order.domain.PaymentStatus
import delivery.order.infrastructure.OrderRepository
import delivery.shop.application.MenuService
import delivery.shop.application.ShopService
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

        // ⚠️ 의도적 구식 구현 — Phase 3 B-6에서 JDBC batch insert로 개선 예정.
        //   장바구니 항목 수만큼 INSERT가 건별로 나간다.
        val orders = cart.items.map { item ->
            orderRepository.save(
                Order(
                    customerId = command.customerId,
                    shopId = cart.cart.shopId,
                    menuId = item.menuId,
                    menuName = item.menuName,
                    menuPrice = item.menuPrice,
                    quantity = item.quantity,
                    customerName = command.customerName,
                    customerPhone = command.customerPhone,
                )
            )
        }

        val payment = paymentService.requestPayment(
            RequestPaymentCommand(orderId = orders.first().id!!, amount = cart.totalPrice)
        )

        val nextStatus = if (payment.status == PaymentStatus.APPROVED) OrderStatus.PAID else OrderStatus.PAYMENT_FAILED
        orders.forEach { it.transitionTo(nextStatus) }

        if (nextStatus == OrderStatus.PAID) {
            cartService.clear(command.customerId)
        }

        return OrderResult(orders, payment)
    }

    // ⚠️ 의도적 구식 구현 — Phase 3 A-4에서 fetch join/batch fetch로 개선 예정.
    //   목록 조회 시 각 주문의 연관 정보를 별도로 조회하면 N+1이 발생한다(주문 자체는
    //   단일 테이블 조회라 지금 당장은 N+1이 없지만, Phase 1 다른 화면에서 재현한다).
    fun getMyOrders(customerId: Long): List<Order> = orderRepository.findAllByCustomerId(customerId)

    fun getOrder(orderId: Long, requester: AuthenticatedUser): Order {
        val order = orderRepository.findById(orderId).orElseThrow { BusinessException(OrderErrorCode.ORDER_NOT_FOUND) }
        if (order.customerId != requester.userId) {
            throw BusinessException(OrderErrorCode.ORDER_NOT_FOUND)
        }
        return order
    }
}
