package delivery.settlement.domain

import delivery.common.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettlementTest {

    private val periodStart: Instant = Instant.parse("2026-03-01T00:00:00Z")
    private val periodEnd: Instant = periodStart.plus(30, ChronoUnit.DAYS)

    private fun newSettlement(): Settlement = Settlement(
        targetType = SettlementTargetType.SHOP,
        targetId = 1L,
        periodStart = periodStart,
        periodEnd = periodEnd,
        totalAmount = 0L,
    )

    @Test
    fun `신규 생성 시 id는 null이다`() {
        assertNull(newSettlement().id)
    }

    @Test
    fun `신규 생성 시 기본 상태는 PENDING이다`() {
        assertEquals(SettlementStatus.PENDING, newSettlement().status)
    }

    @Test
    fun `기간의 시작과 종료가 같으면 생성에 실패한다`() {
        assertThrows<IllegalArgumentException> {
            Settlement(
                targetType = SettlementTargetType.SHOP,
                targetId = 1L,
                periodStart = periodStart,
                periodEnd = periodStart,
                totalAmount = 0L,
            )
        }
    }

    @Test
    fun `기간의 시작이 종료보다 늦으면 생성에 실패한다`() {
        assertThrows<IllegalArgumentException> {
            Settlement(
                targetType = SettlementTargetType.SHOP,
                targetId = 1L,
                periodStart = periodEnd,
                periodEnd = periodStart,
                totalAmount = 0L,
            )
        }
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val settlement = Settlement.withId(
            id = 10L,
            targetType = SettlementTargetType.RIDER,
            targetId = 1L,
            periodStart = periodStart,
            periodEnd = periodEnd,
        )

        assertEquals(10L, settlement.id)
    }

    @Test
    fun `허용된 전이면 상태가 바뀐다`() {
        val settlement = newSettlement()

        settlement.transitionTo(SettlementStatus.CONFIRMED)

        assertEquals(SettlementStatus.CONFIRMED, settlement.status)
    }

    @Test
    fun `허용되지 않은 전이는 예외가 발생하고 상태가 바뀌지 않는다`() {
        val settlement = newSettlement()

        val exception = assertThrows<BusinessException> { settlement.transitionTo(SettlementStatus.PAID) }

        assertEquals(SettlementErrorCode.INVALID_SETTLEMENT_STATUS_TRANSITION, exception.errorCode)
        assertEquals(SettlementStatus.PENDING, settlement.status)
    }
}
