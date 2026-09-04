package delivery.coupon.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

// 매일 새벽, expiresAt이 지난 ISSUED 건을 EXPIRED로 일괄 전환한다(커밋 53-11).
// 정산 배치(새벽 3시)와 자원을 다투지 않도록 새벽 4시로 잡았다. Job 자체를 Spring Boot가
// 부팅할 때마다 자동 실행하지 않도록 spring.batch.job.enabled=false로 꺼두고, 이
// 스케줄러가 명시적으로만 실행한다(53-6과 동일한 이유).
@Component
class CouponExpiryBatchScheduler(
    private val jobLauncher: JobLauncher,
    private val couponExpiryJob: Job,
) {
    private val logger = LoggerFactory.getLogger(CouponExpiryBatchScheduler::class.java)

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun runCouponExpiry() {
        runJob(Instant.now().toString())
    }

    private fun runJob(asOf: String) {
        try {
            jobLauncher.run(couponExpiryJob, JobParametersBuilder().addString("asOf", asOf).toJobParameters())
        } catch (e: Exception) {
            logger.error("쿠폰 만료 배치 실행 실패: asOf={}", asOf, e)
        }
    }
}
