package delivery.settlement.infrastructure

import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementItem
import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementTargetType
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementItemRepositoryIntegrationTest(
    @Autowired private val settlementRepository: SettlementRepository,
    @Autowired private val settlementItemRepository: SettlementItemRepository,
) : IntegrationTestSupport() {

    private val periodStart: Instant = Instant.parse("2026-03-01T00:00:00Z")
    private val periodEnd: Instant = periodStart.plus(30, ChronoUnit.DAYS)

    private fun newSettlement(): Settlement =
        settlementRepository.save(Settlement(SettlementTargetType.SHOP, System.nanoTime(), periodStart, periodEnd, totalAmount = 0L))

    @Test
    fun `정산 항목을 저장하면 id가 채번된다`() {
        val settlement = newSettlement()

        val item = settlementItemRepository.save(
            SettlementItem(settlement.id!!, orderId = 101L, SettlementItemType.SALE, amount = 10_000L, appliedFeeRate = BigDecimal("0.2000"), settlementAmount = 8_000L)
        )

        assertTrue(item.id != null)
    }

    @Test
    fun `정산 단위로 항목 목록을 조회한다`() {
        val settlement = newSettlement()
        val otherSettlement = newSettlement()
        settlementItemRepository.save(
            SettlementItem(settlement.id!!, orderId = 101L, SettlementItemType.SALE, amount = 10_000L, appliedFeeRate = BigDecimal("0.2000"), settlementAmount = 8_000L)
        )
        settlementItemRepository.save(
            SettlementItem(settlement.id!!, orderId = 102L, SettlementItemType.SALE, amount = 20_000L, appliedFeeRate = BigDecimal("0.2000"), settlementAmount = 16_000L)
        )
        settlementItemRepository.save(
            SettlementItem(otherSettlement.id!!, orderId = 103L, SettlementItemType.SALE, amount = 5_000L, appliedFeeRate = BigDecimal("0.2000"), settlementAmount = 4_000L)
        )

        val actual = settlementItemRepository.findAllBySettlementId(settlement.id!!)

        assertEquals(2, actual.size)
        assertTrue(actual.all { it.settlementId == settlement.id })
    }

    @Test
    fun `환불 항목은 정산 기여분이 음수로 저장된다`() {
        val settlement = newSettlement()

        val item = settlementItemRepository.save(
            SettlementItem(settlement.id!!, orderId = 101L, SettlementItemType.REFUND, amount = 10_000L, appliedFeeRate = BigDecimal("0.2000"), settlementAmount = -8_000L)
        )

        assertEquals(SettlementItemType.REFUND, item.type)
        assertTrue(item.settlementAmount < 0)
    }
}
