package delivery.coupon.batch

import delivery.coupon.domain.Coupon
import delivery.coupon.domain.Issuance
import delivery.coupon.domain.IssuanceStatus
import delivery.coupon.infrastructure.CouponRepository
import delivery.coupon.infrastructure.IssuanceRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.time.Instant
import kotlin.test.assertEquals

// 53-11 배치(Job) 전체가 실제로 리더/프로세서/커밋 경계까지 맞물려 도는지 검증한다.
@SpringBatchTest
class CouponExpiryBatchJobIntegrationTest(
    @Autowired private val jobLauncherTestUtils: JobLauncherTestUtils,
    @Autowired @Qualifier("couponExpiryJob") private val couponExpiryJob: Job,
    @Autowired private val couponRepository: CouponRepository,
    @Autowired private val issuanceRepository: IssuanceRepository,
) : IntegrationTestSupport() {

    private fun newCoupon(): Coupon =
        couponRepository.save(Coupon(name = "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7))

    private fun issuanceExpiringAt(couponId: Long, userId: Long, expiresAt: Instant): Issuance =
        issuanceRepository.save(Issuance(userId = userId, couponId = couponId, issuedAt = expiresAt.minusSeconds(7 * 86400), validityDays = 7))

    @Test
    fun `만료시각이 지난 ISSUED 건을 EXPIRED로 전환한다`() {
        val coupon = newCoupon()
        val now = Instant.now()
        val expired = issuanceExpiringAt(coupon.id!!, userId = System.nanoTime(), expiresAt = now.minusSeconds(3600))

        jobLauncherTestUtils.job = couponExpiryJob
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder().addString("asOf", now.toString()).addLong("uniquifier", System.nanoTime()).toJobParameters()
        )

        assertEquals(BatchStatus.COMPLETED, execution.status)
        val actual = issuanceRepository.findById(expired.id!!).get()
        assertEquals(IssuanceStatus.EXPIRED, actual.status)
    }

    @Test
    fun `아직 만료되지 않은 ISSUED 건은 그대로 둔다`() {
        val coupon = newCoupon()
        val now = Instant.now()
        val notExpired = issuanceExpiringAt(coupon.id!!, userId = System.nanoTime(), expiresAt = now.plusSeconds(3600))

        jobLauncherTestUtils.job = couponExpiryJob
        jobLauncherTestUtils.launchJob(
            JobParametersBuilder().addString("asOf", now.toString()).addLong("uniquifier", System.nanoTime()).toJobParameters()
        )

        val actual = issuanceRepository.findById(notExpired.id!!).get()
        assertEquals(IssuanceStatus.ISSUED, actual.status)
    }

    @Test
    fun `이미 사용된 건은 만료시각이 지났어도 건드리지 않고 넘어간다`() {
        val coupon = newCoupon()
        val now = Instant.now()
        val used = issuanceExpiringAt(coupon.id!!, userId = System.nanoTime(), expiresAt = now.minusSeconds(3600))
        used.use()
        issuanceRepository.save(used)
        val stillPending = issuanceExpiringAt(coupon.id!!, userId = System.nanoTime(), expiresAt = now.minusSeconds(3600))

        jobLauncherTestUtils.job = couponExpiryJob
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder().addString("asOf", now.toString()).addLong("uniquifier", System.nanoTime()).toJobParameters()
        )

        assertEquals(BatchStatus.COMPLETED, execution.status)
        assertEquals(IssuanceStatus.USED, issuanceRepository.findById(used.id!!).get().status)
        assertEquals(IssuanceStatus.EXPIRED, issuanceRepository.findById(stillPending.id!!).get().status)
    }
}
