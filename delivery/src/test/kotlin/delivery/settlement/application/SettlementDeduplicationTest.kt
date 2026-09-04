package delivery.settlement.application

import delivery.common.exception.BusinessException
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementErrorCode
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.SettlementRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import kotlin.test.assertEquals

class SettlementDeduplicationTest {

    private val settlementRepository = mockk<SettlementRepository>()

    private fun newSettlement(): Settlement =
        Settlement(SettlementTargetType.SHOP, 1L, Instant.EPOCH, Instant.EPOCH.plusSeconds(1), totalAmount = 0L)

    @Test
    fun `저장에 성공하면 저장된 정산을 그대로 반환한다`() {
        val settlement = newSettlement()
        every { settlementRepository.save(settlement) } returns settlement

        val actual = settlementRepository.saveOrThrowDuplicate(settlement)

        assertEquals(settlement, actual)
    }

    @Test
    fun `유니크 제약 위반이면 SETTLEMENT_ALREADY_EXISTS로 변환한다`() {
        val settlement = newSettlement()
        every { settlementRepository.save(settlement) } throws DataIntegrityViolationException("uk_settlement_target_period")

        val exception = assertThrows<BusinessException> { settlementRepository.saveOrThrowDuplicate(settlement) }

        assertEquals(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS, exception.errorCode)
    }
}
