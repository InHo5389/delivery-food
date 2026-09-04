package delivery.coupon.batch

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import java.time.Instant

class CouponExpiryBatchSchedulerTest {

    private val jobLauncher = mockk<JobLauncher>()

    // relaxed=true: 실패 로깅 경로에서 job.name을 참조하는데, 이 테스트들의 관심사가
    // 아니라서 굳이 스텁하지 않고 기본값을 받는다(53-6 SettlementBatchSchedulerTest와 동일한 이유).
    private val couponExpiryJob = mockk<Job>(relaxed = true)
    private val scheduler = CouponExpiryBatchScheduler(jobLauncher, couponExpiryJob)

    @Test
    fun `쿠폰 만료 배치는 현재 시각을 asOf 파라미터로 실행한다`() {
        val paramsSlot = slot<JobParameters>()
        every { jobLauncher.run(couponExpiryJob, capture(paramsSlot)) } returns mockk<JobExecution>()

        val before = Instant.now()
        scheduler.runCouponExpiry()
        val after = Instant.now()

        val asOf = Instant.parse(paramsSlot.captured.getString("asOf"))
        kotlin.test.assertTrue(!asOf.isBefore(before) && !asOf.isAfter(after))
    }

    @Test
    fun `잡 실행 중 예외가 발생해도 스케줄러 밖으로 전파되지 않는다`() {
        every { jobLauncher.run(couponExpiryJob, any()) } throws RuntimeException("배치 실행 실패")

        scheduler.runCouponExpiry()

        verify(exactly = 1) { jobLauncher.run(couponExpiryJob, any()) }
    }
}
