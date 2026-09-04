package delivery.settlement.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

private val KST = ZoneId.of("Asia/Seoul")

// 정산 배치 실행 주기(53-6): 라이더는 매일 새벽 3시(전일 집계), 상점은 매주 월요일
// 새벽 3시(지난주 월~일 집계). Job 자체를 Spring Boot가 부팅할 때마다 자동 실행하지
// 않도록 spring.batch.job.enabled=false로 꺼두고, 이 스케줄러가 명시적으로만 실행한다.
@Component
class SettlementBatchScheduler(
    private val jobLauncher: JobLauncher,
    private val riderSettlementJob: Job,
    private val shopSettlementJob: Job,
) {
    private val logger = LoggerFactory.getLogger(SettlementBatchScheduler::class.java)

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    fun runRiderSettlement() {
        val yesterday = LocalDate.now(KST).minusDays(1)
        runJob(riderSettlementJob, "date", yesterday.toString())
    }

    @Scheduled(cron = "0 0 3 * * MON", zone = "Asia/Seoul")
    fun runShopSettlement() {
        val lastMonday = LocalDate.now(KST).minusWeeks(1).with(DayOfWeek.MONDAY)
        runJob(shopSettlementJob, "weekStart", lastMonday.toString())
    }

    // 같은 파라미터로 이미 완료된 Job을 다시 실행하면 Spring Batch가
    // JobInstanceAlreadyCompleteException을 던진다 — 이것도 하루/한 주에 한 번만
    // 돌아야 하는 이 배치에는 유효한 중복 방지선이라 에러로 취급하지 않고 로그만 남긴다.
    private fun runJob(job: Job, paramName: String, paramValue: String) {
        try {
            jobLauncher.run(job, JobParametersBuilder().addString(paramName, paramValue).toJobParameters())
        } catch (e: Exception) {
            logger.error("정산 배치 실행 실패: job={}, {}={}", job.name, paramName, paramValue, e)
        }
    }
}
