package delivery.settlement.infrastructure

import delivery.settlement.domain.CommissionRate
import delivery.settlement.domain.RateType
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

// rate_type이 지금은 PLATFORM_FEE 하나뿐이라(RateType.kt), 테스트 간 DB 정리 없이 같은
// 클래스의 여러 테스트가 이 테이블을 공유하면 서로 다른 테스트가 저장한 요율이 다른
// 테스트의 시간 범위 질의에 섞여 들어올 수 있다. 그래서 테스트마다 겹치지 않는 랜덤
// 기준 시각(uniqueBase)에서 오프셋을 잡아 서로 다른 테스트의 데이터가 절대 같은 질의
// 범위에 들어오지 않도록 한다(다른 테스트들의 System.nanoTime() 기반 고유 ID 패턴과 동일한 이유).
class CommissionRateRepositoryIntegrationTest(
    @Autowired private val commissionRateRepository: CommissionRateRepository,
) : IntegrationTestSupport() {

    private fun uniqueBase(): Instant = Instant.EPOCH.plusSeconds(System.nanoTime())

    @Test
    fun `요율을 저장하면 id가 채번된다`() {
        val rate = commissionRateRepository.save(
            CommissionRate(RateType.PLATFORM_FEE, BigDecimal("0.2000"), uniqueBase())
        )

        assertEquals(BigDecimal("0.2000"), rate.rate)
    }

    @Test
    fun `유효 시작 시각 이전 시점으로 조회하면 null을 반환한다`() {
        val effectiveFrom = uniqueBase()
        commissionRateRepository.save(CommissionRate(RateType.PLATFORM_FEE, BigDecimal("0.2000"), effectiveFrom))

        val actual = commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            RateType.PLATFORM_FEE, effectiveFrom.minusSeconds(1),
        )

        assertNull(actual)
    }

    @Test
    fun `유효 시작 시각과 정확히 같은 시점으로 조회하면 해당 요율을 반환한다`() {
        val effectiveFrom = uniqueBase()
        commissionRateRepository.save(CommissionRate(RateType.PLATFORM_FEE, BigDecimal("0.2000"), effectiveFrom))

        val actual = commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            RateType.PLATFORM_FEE, effectiveFrom,
        )

        assertEquals(BigDecimal("0.2000"), actual?.rate)
    }

    @Test
    fun `여러 요율 이력 중 조회 시점 이하 가장 최근 요율을 반환한다`() {
        val base = uniqueBase()
        val marchStart = base
        val aprilStart = base.plusSeconds(3600)
        commissionRateRepository.save(CommissionRate(RateType.PLATFORM_FEE, BigDecimal("0.2000"), marchStart))
        commissionRateRepository.save(CommissionRate(RateType.PLATFORM_FEE, BigDecimal("0.1800"), aprilStart))

        val beforeApril = commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            RateType.PLATFORM_FEE, aprilStart.minusSeconds(1),
        )
        val afterApril = commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            RateType.PLATFORM_FEE, aprilStart.plusSeconds(1),
        )

        assertEquals(BigDecimal("0.2000"), beforeApril?.rate)
        assertEquals(BigDecimal("0.1800"), afterApril?.rate)
    }

    @Test
    fun `아직 어떤 요율도 등록되지 않은 시점으로 조회하면 null을 반환한다`() {
        val actual = commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            RateType.PLATFORM_FEE, Instant.EPOCH,
        )

        assertNull(actual)
    }
}
