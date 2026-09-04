package delivery.order.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.delivery.application.DeliveryService
import delivery.delivery.application.dto.CreateDeliveryCommand
import delivery.order.application.dto.CreateOrderCommand
import delivery.order.application.dto.OrderHistoryItem
import delivery.order.application.dto.OrderHistoryQuery
import delivery.order.application.dto.OrderHistoryResult
import delivery.order.application.dto.OrderItemSummary
import delivery.order.application.dto.OrderResult
import delivery.order.application.dto.RequestPaymentCommand
import delivery.order.application.dto.SalesSummaryQuery
import delivery.order.application.dto.SalesSummaryResult
import delivery.order.application.dto.ShopSettlementSourceItem
import delivery.order.domain.CartErrorCode
import delivery.order.domain.Order
import delivery.order.domain.OrderErrorCode
import delivery.order.domain.OrderItem
import delivery.order.domain.OrderStatus
import delivery.order.domain.PaymentStatus
import delivery.order.infrastructure.OrderItemRepository
import delivery.order.infrastructure.OrderRepository
import delivery.order.infrastructure.SalesSummaryRepository
import delivery.order.infrastructure.ShopSettlementSourceRepository
import delivery.shop.application.MenuService
import delivery.shop.application.OrderTicketService
import delivery.shop.application.ShopService
import delivery.shop.application.dto.CreateOrderTicketCommand
import delivery.shop.domain.Shop
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// 사장님이 이 시간 안에 수락/거절하지 않으면 자동 취소 대상이 된다.
private const val STALE_ORDER_THRESHOLD_MINUTES = 3L

// 매출 집계는 "오늘" 같은 하루 단위 조회가 기본이라 한국 사용자 기준 KST로 하루 경계를 계산한다.
private val KST = ZoneId.of("Asia/Seoul")

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
    private val orderTicketService: OrderTicketService,
    private val deliveryService: DeliveryService,
    private val salesSummaryRepository: SalesSummaryRepository,
    private val shopSettlementSourceRepository: ShopSettlementSourceRepository,
) {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

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
                deliveryFee = shop.deliveryFee,
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

        // 결제 금액은 메뉴 합계 + 배달비다 — 배달비는 상점이 아니라 라이더 몫이지만
        // (01_설계원칙.md), 고객에게는 한 번에 청구된다.
        val payment = paymentService.requestPayment(
            RequestPaymentCommand(orderId = order.id!!, amount = cart.totalPrice + shop.deliveryFee)
        )

        val nextStatus = if (payment.status == PaymentStatus.APPROVED) OrderStatus.PAID else OrderStatus.PAYMENT_FAILED
        order.transitionTo(nextStatus)

        if (nextStatus == OrderStatus.PAID) {
            cartService.clear(command.customerId)
            // 사장님이 접수 여부를 판단할 수 있도록 shop 모듈에 주문 티켓을 만들어둔다.
            // shop이 order를 다시 호출하지 않도록(순환 참조 방지) 이 방향(order → shop)의
            // 호출로만 티켓을 생성하고, 이후 수락/거절/조리 액션은 order 모듈이 주도한다.
            orderTicketService.createTicket(
                CreateOrderTicketCommand(
                    orderId = order.id!!,
                    shopId = order.shopId,
                    customerName = order.customerName,
                    totalAmount = cart.totalPrice,
                )
            )
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

    // shop 모듈처럼 다른 모듈이 주문 항목을 조회할 때 쓴다 — 엔티티(OrderItem)를 그대로
    // 넘기면 모듈 경계 규칙(DTO만 주고받기)을 어기게 되므로 별도 DTO로 변환해서 반환한다.
    fun getOrderItemSummaries(orderId: Long): List<OrderItemSummary> =
        orderItemRepository.findAllByOrderId(orderId).map { OrderItemSummary(it.menuName, it.menuPrice, it.quantity) }

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
            // 티켓은 PAID 시점에만 만들어지므로 wasPaid일 때만 정리 대상이 존재한다.
            orderTicketService.markCancelled(order.id!!)
        }

        return order
    }

    // 사장님이 STALE_ORDER_THRESHOLD_MINUTES 안에 반응하지 않은 주문을 스케줄러가 대신
    // 취소한다 — 고객이 무한정 기다리지 않도록. 주문 단위로 트랜잭션을 나눠(autoCancelIfStale)
    // 한 건의 실패가 같은 사이클의 다른 자동 취소를 막지 않게 한다.
    fun autoCancelStaleOrders(): List<Long> {
        val threshold = Instant.now().minus(STALE_ORDER_THRESHOLD_MINUTES, ChronoUnit.MINUTES)
        return orderRepository.findAllByStatusAndUpdatedAtBefore(OrderStatus.PAID, threshold).mapNotNull { stale ->
            runCatching { autoCancelIfStale(stale.id!!) }
                .onFailure { logger.error("미접수 주문 자동 취소 실패: orderId={}", stale.id, it) }
                .map { stale.id }
                .getOrNull()
        }
    }

    // 조회와 취소 사이에 사장님이 먼저 수락/거절했을 수 있으므로 재확인 후에도 여전히
    // PAID일 때만 취소한다 — 그 사이 상태가 바뀌었다면 조용히 넘어간다(오류 아님).
    @Transactional
    fun autoCancelIfStale(orderId: Long) {
        val order = orderRepository.findById(orderId).orElseThrow { BusinessException(OrderErrorCode.ORDER_NOT_FOUND) }
        if (order.status != OrderStatus.PAID) {
            return
        }
        order.transitionTo(OrderStatus.CANCELLED)
        paymentService.refund(orderId)
        orderTicketService.markCancelled(orderId)
    }

    // 배차는 조리 완료(COOKED)가 아니라 접수(ACCEPTED) 시점에 시작한다 — 조리 시간에
    // 맞춰 라이더가 도착하도록 역산하는 것이 목표다. COOKED 시점에 시작하면 라이더가
    // 도착할 때까지 음식이 식고, PAID 시점에 시작하면 사장님이 아직 거절할 수 있는데도
    // 미리 라이더를 부르게 된다.
    @Transactional
    fun acceptOrder(orderId: Long, requester: AuthenticatedUser, estimatedCookingMinutes: Int): Order {
        val (order, shop) = getOrderForOwner(orderId, requester)
        order.transitionTo(OrderStatus.ACCEPTED)
        orderTicketService.markAccepted(orderId)
        deliveryService.createDelivery(
            CreateDeliveryCommand(
                orderId = order.id!!,
                shopId = shop.id!!,
                pickupLatitude = shop.latitude,
                pickupLongitude = shop.longitude,
                estimatedCookingMinutes = estimatedCookingMinutes,
            )
        )
        return order
    }

    // 이미 결제된 주문을 사장님이 거절하는 것이므로 취소와 마찬가지로 환불이 필요하다.
    @Transactional
    fun rejectOrder(orderId: Long, requester: AuthenticatedUser): Order {
        val (order, _) = getOrderForOwner(orderId, requester)
        order.transitionTo(OrderStatus.REJECTED)
        orderTicketService.markRejected(orderId)
        paymentService.refund(orderId)
        return order
    }

    @Transactional
    fun startCooking(orderId: Long, requester: AuthenticatedUser): Order {
        val (order, _) = getOrderForOwner(orderId, requester)
        order.transitionTo(OrderStatus.COOKING)
        orderTicketService.markCookingStarted(orderId)
        return order
    }

    @Transactional
    fun finishCooking(orderId: Long, requester: AuthenticatedUser): Order {
        val (order, _) = getOrderForOwner(orderId, requester)
        order.transitionTo(OrderStatus.COOKED)
        orderTicketService.markCookingDone(orderId)
        return order
    }

    // delivery 모듈이 라이더 배정/픽업/배달완료 시점마다 호출한다(delivery → order,
    // 이 세 메서드에서만). order → delivery는 createDelivery 하나뿐이라 서로 다른 방향의
    // 호출이 같은 두 서비스 사이를 왕복하지 않는다 — DeliveryService는 order만 호출하고
    // (order를 다시 호출하지 않음), 이 동기화는 DeliveryFulfillmentService/DispatchOfferService/
    // DispatchQueueService처럼 OrderService가 의존하지 않는 별도 델리버리쪽 서비스에서만 건다.
    @Transactional
    fun markRiderAssigned(orderId: Long) {
        val order = orderRepository.findById(orderId).orElseThrow { BusinessException(OrderErrorCode.ORDER_NOT_FOUND) }
        order.transitionTo(OrderStatus.RIDER_ASSIGNED)
    }

    @Transactional
    fun markPickedUp(orderId: Long) {
        val order = orderRepository.findById(orderId).orElseThrow { BusinessException(OrderErrorCode.ORDER_NOT_FOUND) }
        order.transitionTo(OrderStatus.PICKED_UP)
    }

    @Transactional
    fun markDelivered(orderId: Long) {
        val order = orderRepository.findById(orderId).orElseThrow { BusinessException(OrderErrorCode.ORDER_NOT_FOUND) }
        order.transitionTo(OrderStatus.DELIVERED)
    }

    // "완료된" 매출만 집계 대상으로 본다 — 배달 중이거나 아직 픽업 전인 주문은 취소될
    // 여지가 있어 확정된 매출이 아니다. 반열림 구간([start, end))으로 KST 하루를 끊는다.
    fun getSalesSummary(query: SalesSummaryQuery, requester: AuthenticatedUser): SalesSummaryResult {
        val shop = shopService.getById(query.shopId)
        if (requester.role != Role.OWNER || shop.ownerId != requester.userId) {
            throw BusinessException(OrderErrorCode.NOT_SHOP_OWNER)
        }
        val start = query.date.atStartOfDay(KST).toInstant()
        val end = query.date.plusDays(1).atStartOfDay(KST).toInstant()
        val row = salesSummaryRepository.findSales(query.shopId, start, end)
        return SalesSummaryResult(date = query.date, orderCount = row.orderCount, totalAmount = row.totalAmount)
    }

    // settlement 모듈이 상점 정산을 계산할 때 쓴다(주문 하나하나에 요율을 적용해야 하므로
    // getSalesSummary처럼 합계 하나가 아니라 주문 단위 목록을 돌려준다).
    fun getDeliveredOrderAmounts(shopId: Long, from: Instant, to: Instant): List<ShopSettlementSourceItem> =
        shopSettlementSourceRepository.findDeliveredOrderAmounts(shopId, from, to)
            .map { ShopSettlementSourceItem(it.orderId, it.amount) }

    // 환불 대상은 Order.status가 아니라 Payment.status=REFUNDED로 판정한다
    // (ShopSettlementSourceRepository 주석 참조 — 결제 전 취소와 실제 환불을 구분하기 위함).
    fun getRefundedPaymentAmounts(shopId: Long, from: Instant, to: Instant): List<ShopSettlementSourceItem> =
        shopSettlementSourceRepository.findRefundedPaymentAmounts(shopId, from, to)
            .map { ShopSettlementSourceItem(it.orderId, it.amount) }

    // settlement 모듈이 라이더 정산을 계산할 때 쓴다. 배달비는 라이더 몫으로, 플랫폼
    // 수수료 대상이 아니라 요율 적용 없이 스냅샷된 금액을 그대로 돌려준다.
    fun getDeliveryFees(orderIds: List<Long>): Map<Long, Long> =
        orderRepository.findAllById(orderIds).associate { it.id!! to it.deliveryFee }

    private fun getOrderForOwner(orderId: Long, requester: AuthenticatedUser): Pair<Order, Shop> {
        val order = orderRepository.findById(orderId).orElseThrow { BusinessException(OrderErrorCode.ORDER_NOT_FOUND) }
        val shop = shopService.getById(order.shopId)
        if (requester.role != Role.OWNER || shop.ownerId != requester.userId) {
            throw BusinessException(OrderErrorCode.NOT_SHOP_OWNER)
        }
        return order to shop
    }
}
