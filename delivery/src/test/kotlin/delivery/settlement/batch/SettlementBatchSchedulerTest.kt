package delivery.settlement.batch

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class SettlementBatchSchedulerTest {

    private val jobLauncher = mockk<JobLauncher>()

    // relaxed=true: 실패 로깅 경로에서 job.name(Job 인터페이스의 getName())을 참조하는데,
    // 이 테스트들의 관심사가 아니라서 굳이 스텁하지 않고 기본값을 받는다.
    private val riderSettlementJob = mockk<Job>(relaxed = true)
    private val shopSettlementJob = mockk<Job>(relaxed = true)
    private val scheduler = SettlementBatchScheduler(jobLauncher, riderSettlementJob, shopSettlementJob)

    private val zone = ZoneId.of("Asia/Seoul")

    @Test
    fun `라이더 정산 배치는 전일 날짜를 파라미터로 실행한다`() {
        val paramsSlot = slot<JobParameters>()
        every { jobLauncher.run(riderSettlementJob, capture(paramsSlot)) } returns mockk<JobExecution>()

        scheduler.runRiderSettlement()

        val expectedYesterday = LocalDate.now(zone).minusDays(1)
        assertEquals(expectedYesterday.toString(), paramsSlot.captured.getString("date"))
    }

    @Test
    fun `상점 정산 배치는 지난주 월요일 날짜를 파라미터로 실행한다`() {
        val paramsSlot = slot<JobParameters>()
        every { jobLauncher.run(shopSettlementJob, capture(paramsSlot)) } returns mockk<JobExecution>()

        scheduler.runShopSettlement()

        val expectedLastMonday = LocalDate.now(zone).minusWeeks(1).with(DayOfWeek.MONDAY)
        assertEquals(expectedLastMonday.toString(), paramsSlot.captured.getString("weekStart"))
        assertEquals(DayOfWeek.MONDAY, expectedLastMonday.dayOfWeek)
    }

    @Test
    fun `잡 실행 중 예외가 발생해도 스케줄러 밖으로 전파되지 않는다`() {
        every { jobLauncher.run(riderSettlementJob, any()) } throws RuntimeException("배치 실행 실패")

        scheduler.runRiderSettlement()

        verify(exactly = 1) { jobLauncher.run(riderSettlementJob, any()) }
    }
}
