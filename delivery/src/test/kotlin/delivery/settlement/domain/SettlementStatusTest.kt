package delivery.settlement.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettlementStatusTest {

    @Test
    fun `PENDING에서 CONFIRMED로 전이할 수 있다`() {
        assertTrue(SettlementStatus.PENDING.canTransitionTo(SettlementStatus.CONFIRMED))
    }

    @Test
    fun `PENDING에서 PAID로 직접 전이할 수 없다`() {
        assertFalse(SettlementStatus.PENDING.canTransitionTo(SettlementStatus.PAID))
    }

    @Test
    fun `CONFIRMED에서 PAID로 전이할 수 있다`() {
        assertTrue(SettlementStatus.CONFIRMED.canTransitionTo(SettlementStatus.PAID))
    }

    @Test
    fun `CONFIRMED에서 PENDING으로 되돌릴 수 없다`() {
        assertFalse(SettlementStatus.CONFIRMED.canTransitionTo(SettlementStatus.PENDING))
    }

    @Test
    fun `PAID는 최종 상태라 어디로도 전이할 수 없다`() {
        for (target in SettlementStatus.entries) {
            assertFalse(SettlementStatus.PAID.canTransitionTo(target))
        }
    }

    @Test
    fun `같은 상태로의 전이는 허용되지 않는다`() {
        for (status in SettlementStatus.entries) {
            assertFalse(status.canTransitionTo(status))
        }
    }
}
