package delivery.delivery.infrastructure

import delivery.delivery.domain.Rider
import delivery.delivery.domain.RiderStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertTrue

class RiderCandidateRepositoryIntegrationTest(
    @Autowired private val riderRepository: RiderRepository,
    @Autowired private val riderCandidateRepository: RiderCandidateRepository,
) : IntegrationTestSupport() {

    // 서울시청 좌표를 기준점(배달 픽업 좌표)으로 사용
    private val pickupLat = 37.5665
    private val pickupLng = 126.9780

    @Test
    fun `반경 내 AVAILABLE 라이더만 후보로 조회된다`() {
        val near = riderRepository.save(
            Rider(System.nanoTime(), BigDecimal("37.5670000"), BigDecimal("126.9785000"), status = RiderStatus.AVAILABLE)
        )

        val actual = riderCandidateRepository.findAvailableCandidates(pickupLat, pickupLng, radiusMeters = 3000.0)

        assertTrue(actual.any { it.riderId == near.id })
    }

    @Test
    fun `반경 밖 라이더는 후보에서 제외된다`() {
        val far = riderRepository.save(
            Rider(System.nanoTime(), BigDecimal("35.1796000"), BigDecimal("129.0756000"), status = RiderStatus.AVAILABLE)
        )

        val actual = riderCandidateRepository.findAvailableCandidates(pickupLat, pickupLng, radiusMeters = 3000.0)

        assertTrue(actual.none { it.riderId == far.id })
    }

    @Test
    fun `BUSY 라이더는 반경 안에 있어도 후보에서 제외된다`() {
        val busy = riderRepository.save(
            Rider(System.nanoTime(), BigDecimal("37.5666000"), BigDecimal("126.9781000"), status = RiderStatus.BUSY)
        )

        val actual = riderCandidateRepository.findAvailableCandidates(pickupLat, pickupLng, radiusMeters = 3000.0)

        assertTrue(actual.none { it.riderId == busy.id })
    }

    @Test
    fun `OFFLINE 라이더는 반경 안에 있어도 후보에서 제외된다`() {
        val offline = riderRepository.save(
            Rider(System.nanoTime(), BigDecimal("37.5666000"), BigDecimal("126.9781000"), status = RiderStatus.OFFLINE)
        )

        val actual = riderCandidateRepository.findAvailableCandidates(pickupLat, pickupLng, radiusMeters = 3000.0)

        assertTrue(actual.none { it.riderId == offline.id })
    }

    @Test
    fun `반경 정확히 경계에 걸친 라이더 여부는 radiusMeters를 넓히면 포함된다`() {
        val far = riderRepository.save(
            Rider(System.nanoTime(), BigDecimal("35.1796000"), BigDecimal("129.0756000"), status = RiderStatus.AVAILABLE)
        )

        val actual = riderCandidateRepository.findAvailableCandidates(pickupLat, pickupLng, radiusMeters = 500_000.0)

        assertTrue(actual.any { it.riderId == far.id })
    }
}
