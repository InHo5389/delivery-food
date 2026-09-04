package delivery.settlement.infrastructure

import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementTargetType
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SettlementRepositoryIntegrationTest(
    @Autowired private val settlementRepository: SettlementRepository,
) : IntegrationTestSupport() {

    private val periodStart: Instant = Instant.parse("2026-03-01T00:00:00Z")
    private val periodEnd: Instant = periodStart.plus(30, ChronoUnit.DAYS)

    @Test
    fun `정산을 저장하면 id가 채번된다`() {
        val targetId = System.nanoTime()
        val settlement = settlementRepository.save(
            Settlement(SettlementTargetType.SHOP, targetId, periodStart, periodEnd, totalAmount = 24_000L)
        )

        assertNotNull(settlement.id)
    }

    @Test
    fun `같은 대상 같은 기간의 정산을 조회할 수 있다`() {
        val targetId = System.nanoTime()
        val saved = settlementRepository.save(
            Settlement(SettlementTargetType.SHOP, targetId, periodStart, periodEnd, totalAmount = 24_000L)
        )

        val actual = settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(
            SettlementTargetType.SHOP, targetId, periodStart, periodEnd,
        )

        assertEquals(saved.id, actual?.id)
    }

    @Test
    fun `존재하지 않는 대상 기간으로 조회하면 null을 반환한다`() {
        val actual = settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(
            SettlementTargetType.SHOP, Long.MAX_VALUE, periodStart, periodEnd,
        )

        assertNull(actual)
    }

    @Test
    fun `대상별 정산 목록을 기간 최신순으로 조회한다`() {
        val targetId = System.nanoTime()
        val marchStart = periodStart
        val marchEnd = periodEnd
        val aprilStart = marchEnd
        val aprilEnd = aprilStart.plus(30, ChronoUnit.DAYS)

        settlementRepository.save(Settlement(SettlementTargetType.SHOP, targetId, marchStart, marchEnd, totalAmount = 10_000L))
        val april = settlementRepository.save(
            Settlement(SettlementTargetType.SHOP, targetId, aprilStart, aprilEnd, totalAmount = 5_000L)
        )
        settlementRepository.save(Settlement(SettlementTargetType.RIDER, targetId, marchStart, marchEnd, totalAmount = 3_000L))

        val actual = settlementRepository.findAllByTargetTypeAndTargetIdOrderByPeriodStartDesc(SettlementTargetType.SHOP, targetId)

        assertEquals(2, actual.size)
        assertEquals(april.id, actual[0].id)
    }

    // V18 마이그레이션의 uk_settlement_target_period 유니크 제약이 실제로 DB 수준에서
    // 중복을 막는지 확인한다 — 53-5에서 애플리케이션 사전 조회 대신 이 제약에 의존하기로
    // 했으므로, 제약 자체가 없으면 SettlementDeduplication.saveOrThrowDuplicate가
    // 아무것도 막지 못한다.
    @Test
    fun `같은 대상 같은 기간의 정산을 두 번 저장하면 유니크 제약 위반이 발생한다`() {
        val targetId = System.nanoTime()
        settlementRepository.save(Settlement(SettlementTargetType.SHOP, targetId, periodStart, periodEnd, totalAmount = 24_000L))

        assertThrows<DataIntegrityViolationException> {
            settlementRepository.saveAndFlush(Settlement(SettlementTargetType.SHOP, targetId, periodStart, periodEnd, totalAmount = 1_000L))
        }
    }
}
