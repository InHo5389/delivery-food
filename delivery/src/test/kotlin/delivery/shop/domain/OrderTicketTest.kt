package delivery.shop.domain

import delivery.common.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrderTicketTest {

    private fun newTicket(): OrderTicket = OrderTicket(
        orderId = 1L,
        shopId = 1L,
        customerName = "홍길동",
        totalAmount = 16000L,
    )

    @Test
    fun `신규 생성 시 id는 null이다`() {
        assertNull(newTicket().id)
    }

    @Test
    fun `신규 생성 시 기본 상태는 PENDING이다`() {
        assertEquals(OrderTicketStatus.PENDING, newTicket().status)
    }

    @Test
    fun `허용된 전이면 상태가 바뀐다`() {
        val ticket = newTicket()

        ticket.transitionTo(OrderTicketStatus.ACCEPTED)

        assertEquals(OrderTicketStatus.ACCEPTED, ticket.status)
    }

    @Test
    fun `허용되지 않은 전이는 예외가 발생하고 상태가 바뀌지 않는다`() {
        val ticket = newTicket()

        val exception = assertThrows<BusinessException> { ticket.transitionTo(OrderTicketStatus.COOKED) }

        assertEquals(OrderTicketErrorCode.INVALID_ORDER_TICKET_STATUS_TRANSITION, exception.errorCode)
        assertEquals(OrderTicketStatus.PENDING, ticket.status)
    }
}
