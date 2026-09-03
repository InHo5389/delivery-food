package delivery.shop.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.shop.application.dto.CreateOrderTicketCommand
import delivery.shop.application.dto.OrderTicketItemCommand
import delivery.shop.domain.OrderTicket
import delivery.shop.domain.OrderTicketErrorCode
import delivery.shop.domain.OrderTicketItem
import delivery.shop.domain.OrderTicketStatus
import delivery.shop.domain.Shop
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.OrderTicketItemRepository
import delivery.shop.infrastructure.OrderTicketRepository
import delivery.shop.infrastructure.ShopRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import kotlin.test.assertEquals

class OrderTicketServiceTest {

    private val orderTicketRepository = mockk<OrderTicketRepository>()
    private val orderTicketItemRepository = mockk<OrderTicketItemRepository>()
    private val shopRepository = mockk<ShopRepository>()
    private lateinit var orderTicketService: OrderTicketService

    @BeforeEach
    fun setUp() {
        orderTicketService = OrderTicketService(orderTicketRepository, orderTicketItemRepository, shopRepository)
    }

    @Test
    fun `티켓을 생성하면 항목도 함께 저장된다`() {
        val ticket = OrderTicket.withId(1L, orderId = 1L, shopId = 1L, totalAmount = 16000L)
        every { orderTicketRepository.save(any()) } returns ticket
        every { orderTicketItemRepository.save(any()) } answers { it.invocation.args[0] as OrderTicketItem }
        val command = CreateOrderTicketCommand(
            orderId = 1L, shopId = 1L, customerName = "홍길동", totalAmount = 16000L,
            items = listOf(OrderTicketItemCommand("짜장면", 8000L, 2)),
        )

        val actual = orderTicketService.createTicket(command)

        assertEquals(1L, actual.id)
        verify(exactly = 1) { orderTicketItemRepository.save(any()) }
    }

    @Test
    fun `존재하지 않는 주문의 티켓을 수락 처리하면 예외가 발생한다`() {
        every { orderTicketRepository.findByOrderId(999L) } returns null

        val exception = assertThrows<BusinessException> { orderTicketService.markAccepted(999L) }

        assertEquals(OrderTicketErrorCode.ORDER_TICKET_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `markAccepted를 호출하면 티켓이 ACCEPTED로 바뀐다`() {
        val ticket = OrderTicket.withId(1L, orderId = 1L, shopId = 1L)
        every { orderTicketRepository.findByOrderId(1L) } returns ticket

        val actual = orderTicketService.markAccepted(1L)

        assertEquals(OrderTicketStatus.ACCEPTED, actual.status)
    }

    @Test
    fun `markRejected를 호출하면 티켓이 REJECTED로 바뀐다`() {
        val ticket = OrderTicket.withId(1L, orderId = 1L, shopId = 1L)
        every { orderTicketRepository.findByOrderId(1L) } returns ticket

        val actual = orderTicketService.markRejected(1L)

        assertEquals(OrderTicketStatus.REJECTED, actual.status)
    }

    @Test
    fun `markCookingStarted와 markCookingDone을 차례로 호출하면 COOKING을 거쳐 COOKED가 된다`() {
        val ticket = OrderTicket.withId(1L, orderId = 1L, shopId = 1L, status = OrderTicketStatus.ACCEPTED)
        every { orderTicketRepository.findByOrderId(1L) } returns ticket

        orderTicketService.markCookingStarted(1L)
        assertEquals(OrderTicketStatus.COOKING, ticket.status)

        orderTicketService.markCookingDone(1L)
        assertEquals(OrderTicketStatus.COOKED, ticket.status)
    }

    @Test
    fun `사장님이 아니면 상점의 티켓 목록을 조회할 수 없다`() {
        every { shopRepository.findById(1L) } returns Optional.of(Shop.withId(1L, 10L, "가게", "서울", "0212345678"))

        val exception = assertThrows<BusinessException> {
            orderTicketService.getForShop(1L, AuthenticatedUser(999L, Role.OWNER))
        }

        assertEquals(ShopErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `사장님이 상점의 티켓 목록을 조회하면 항목까지 포함해서 반환된다`() {
        every { shopRepository.findById(1L) } returns Optional.of(Shop.withId(1L, 10L, "가게", "서울", "0212345678"))
        val ticket = OrderTicket.withId(1L, orderId = 1L, shopId = 1L, customerName = "홍길동", totalAmount = 16000L)
        every { orderTicketRepository.findAllByShopIdOrderByCreatedAtDesc(1L) } returns listOf(ticket)
        every { orderTicketItemRepository.findAllByOrderTicketId(1L) } returns listOf(
            OrderTicketItem.withId(1L, orderTicketId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 2)
        )

        val actual = orderTicketService.getForShop(1L, AuthenticatedUser(10L, Role.OWNER))

        assertEquals(1, actual.size)
        assertEquals(1, actual[0].items.size)
        assertEquals("짜장면", actual[0].items[0].menuName)
    }
}
