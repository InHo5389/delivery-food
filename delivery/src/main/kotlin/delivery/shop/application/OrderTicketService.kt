package delivery.shop.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.shop.application.dto.CreateOrderTicketCommand
import delivery.shop.application.dto.OrderTicketItemResult
import delivery.shop.application.dto.OrderTicketResult
import delivery.shop.domain.OrderTicket
import delivery.shop.domain.OrderTicketErrorCode
import delivery.shop.domain.OrderTicketItem
import delivery.shop.domain.OrderTicketStatus
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.OrderTicketItemRepository
import delivery.shop.infrastructure.OrderTicketRepository
import delivery.shop.infrastructure.ShopRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// order 모듈이 주문 접수·조리 진행 상황을 알려줄 때 호출하는 서비스. 이 방향(order → shop)만
// 존재하고 shop이 order를 다시 호출하지는 않는다 — 양방향으로 열면 두 서비스가 서로를
// 주입하는 순환 참조가 생긴다. 그래서 사장님의 수락/거절/조리 액션 자체는 OrderController
// (order 모듈)에 있고, 이 서비스는 그 결과를 반영만 한다.
@Service
class OrderTicketService(
    private val orderTicketRepository: OrderTicketRepository,
    private val orderTicketItemRepository: OrderTicketItemRepository,
    private val shopRepository: ShopRepository,
) {
    @Transactional
    fun createTicket(command: CreateOrderTicketCommand): OrderTicket {
        val ticket = orderTicketRepository.save(
            OrderTicket(
                orderId = command.orderId,
                shopId = command.shopId,
                customerName = command.customerName,
                totalAmount = command.totalAmount,
            )
        )
        command.items.forEach { item ->
            orderTicketItemRepository.save(
                OrderTicketItem(
                    orderTicketId = ticket.id!!,
                    menuName = item.menuName,
                    menuPrice = item.menuPrice,
                    quantity = item.quantity,
                )
            )
        }
        return ticket
    }

    @Transactional
    fun markAccepted(orderId: Long): OrderTicket = transitionByOrderId(orderId, OrderTicketStatus.ACCEPTED)

    @Transactional
    fun markRejected(orderId: Long): OrderTicket = transitionByOrderId(orderId, OrderTicketStatus.REJECTED)

    @Transactional
    fun markCookingStarted(orderId: Long): OrderTicket = transitionByOrderId(orderId, OrderTicketStatus.COOKING)

    @Transactional
    fun markCookingDone(orderId: Long): OrderTicket = transitionByOrderId(orderId, OrderTicketStatus.COOKED)

    @Transactional
    fun markCancelled(orderId: Long): OrderTicket = transitionByOrderId(orderId, OrderTicketStatus.CANCELLED)

    fun getForShop(shopId: Long, requester: AuthenticatedUser): List<OrderTicketResult> {
        assertShopOwner(shopId, requester)
        return orderTicketRepository.findAllByShopIdOrderByCreatedAtDesc(shopId).map { ticket ->
            toResult(ticket, orderTicketItemRepository.findAllByOrderTicketId(ticket.id!!))
        }
    }

    private fun transitionByOrderId(orderId: Long, next: OrderTicketStatus): OrderTicket {
        val ticket = orderTicketRepository.findByOrderId(orderId)
            ?: throw BusinessException(OrderTicketErrorCode.ORDER_TICKET_NOT_FOUND)
        ticket.transitionTo(next)
        return ticket
    }

    private fun toResult(ticket: OrderTicket, items: List<OrderTicketItem>): OrderTicketResult =
        OrderTicketResult(
            ticketId = ticket.id!!,
            orderId = ticket.orderId,
            shopId = ticket.shopId,
            customerName = ticket.customerName,
            totalAmount = ticket.totalAmount,
            status = ticket.status.name,
            items = items.map { OrderTicketItemResult(it.menuName, it.menuPrice, it.quantity) },
        )

    private fun assertShopOwner(shopId: Long, requester: AuthenticatedUser) {
        val shop = shopRepository.findById(shopId).orElseThrow { BusinessException(ShopErrorCode.SHOP_NOT_FOUND) }
        if (requester.role != Role.OWNER || shop.ownerId != requester.userId) {
            throw BusinessException(ShopErrorCode.NOT_SHOP_OWNER)
        }
    }
}
