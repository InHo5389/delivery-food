package delivery.coupon.infrastructure.batch

import delivery.coupon.domain.Coupon
import delivery.coupon.domain.Issuance
import delivery.coupon.infrastructure.CouponRepository
import delivery.coupon.infrastructure.IssuanceRepository
import delivery.support.IntegrationTestSupport
import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.batch.infrastructure.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpiredIssuanceIdReaderConfigIntegrationTest(
    @Autowired private val couponRepository: CouponRepository,
    @Autowired private val issuanceRepository: IssuanceRepository,
    @Autowired private val entityManagerFactory: EntityManagerFactory,
) : IntegrationTestSupport() {

    private val config = ExpiredIssuanceIdReaderConfig()

    private fun newCoupon(): Coupon =
        couponRepository.save(Coupon(name = "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7))

    // validityDays와 issuedAt을 조합해 expiresAt이 원하는 시각이 되도록 만든다.
    private fun issuanceExpiringAt(couponId: Long, userId: Long, expiresAt: Instant): Issuance =
        issuanceRepository.save(Issuance(userId = userId, couponId = couponId, issuedAt = expiresAt.minusSeconds(7 * 86400), validityDays = 7))

    private fun readAll(asOf: Instant): List<Long> {
        val reader = config.expiredIssuanceIdReader(entityManagerFactory, asOf.toString()) as ItemStreamReader<Long>
        val items = mutableListOf<Long>()
        reader.open(ExecutionContext())
        try {
            while (true) {
                val item = reader.read() ?: break
                items.add(item)
            }
        } finally {
            reader.close()
        }
        return items
    }

    @Test
    fun `ISSUED 상태이면서 만료시각이 지난 발급 건만 반환한다`() {
        val coupon = newCoupon()
        val now = Instant.now()
        val expired = issuanceExpiringAt(coupon.id!!, userId = System.nanoTime(), expiresAt = now.minusSeconds(3600))
        // 범위 밖(아직 만료 전)
        issuanceExpiringAt(coupon.id!!, userId = System.nanoTime(), expiresAt = now.plusSeconds(3600))

        val actual = readAll(now)

        assertEquals(listOf(expired.id), actual)
    }

    @Test
    fun `USED 상태인 발급 건은 만료시각이 지났어도 반환하지 않는다`() {
        val coupon = newCoupon()
        val now = Instant.now()
        val used = issuanceExpiringAt(coupon.id!!, userId = System.nanoTime(), expiresAt = now.minusSeconds(3600))
        used.use()
        issuanceRepository.save(used)

        val actual = readAll(now)

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `만료된 발급 건이 없으면 빈 목록을 반환한다`() {
        val actual = readAll(Instant.now())

        assertTrue(actual.isEmpty())
    }
}
