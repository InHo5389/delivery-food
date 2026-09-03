package delivery.shop.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderTicketStatusTest {

    @Test
    fun `PENDING에서 ACCEPTED로 전이할 수 있다`() {
        assertTrue(OrderTicketStatus.PENDING.canTransitionTo(OrderTicketStatus.ACCEPTED))
    }

    @Test
    fun `PENDING에서 REJECTED로 전이할 수 있다`() {
        assertTrue(OrderTicketStatus.PENDING.canTransitionTo(OrderTicketStatus.REJECTED))
    }

    @Test
    fun `PENDING에서 COOKING으로 직접 전이할 수 없다`() {
        assertFalse(OrderTicketStatus.PENDING.canTransitionTo(OrderTicketStatus.COOKING))
    }

    @Test
    fun `ACCEPTED에서 COOKING으로 전이할 수 있다`() {
        assertTrue(OrderTicketStatus.ACCEPTED.canTransitionTo(OrderTicketStatus.COOKING))
    }

    @Test
    fun `COOKING에서 COOKED로 전이할 수 있다`() {
        assertTrue(OrderTicketStatus.COOKING.canTransitionTo(OrderTicketStatus.COOKED))
    }

    @Test
    fun `REJECTED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in OrderTicketStatus.entries) {
            assertFalse(OrderTicketStatus.REJECTED.canTransitionTo(target))
        }
    }

    @Test
    fun `COOKED는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in OrderTicketStatus.entries) {
            assertFalse(OrderTicketStatus.COOKED.canTransitionTo(target))
        }
    }

    @Test
    fun `같은 상태로의 전이는 허용되지 않는다`() {
        for (status in OrderTicketStatus.entries) {
            assertFalse(status.canTransitionTo(status))
        }
    }
}
