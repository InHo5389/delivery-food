package delivery.settlement.batch

import delivery.common.exception.BusinessException
import delivery.settlement.application.RiderSettlementService
import delivery.settlement.domain.SettlementErrorCode
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate

private const val CHUNK_SIZE = 100

// ⚠️ 의도적 구식 구현(04_Phase1_기본틀.md 53-6 참조) — 정산 대상 라이더를 건별로 순회하며
// 각각 calculateRiderSettlement()를 호출한다(집계 쿼리 하나로 한 번에 처리하지 않음).
// Phase 3에서 이 배치가 6-5절 "배치 Reader 비교(O(N²) 문제)"의 실측 대상이 된다.
//
// calculateRiderSettlement() 자체가 이미 Settlement/SettlementItem을 저장하는 완결된
// @Transactional 단위라서, 이 Step의 ItemWriter는 할 일이 없다(프로세서가 사실상
// reader→계산→저장까지 다 한다 — chunk 커밋 경계만 여러 라이더를 묶는 역할).
@Configuration
class RiderSettlementBatchConfig(
    private val riderSettlementService: RiderSettlementService,
) {
    @Bean
    fun riderSettlementJob(jobRepository: JobRepository, riderSettlementStep: Step): Job =
        JobBuilder("riderSettlementJob", jobRepository)
            .start(riderSettlementStep)
            .build()

    @Bean
    @JobScope
    fun riderSettlementStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        deliveredRiderIdReader: ItemReader<Long>,
        @Value("#{jobParameters['date']}") date: String,
    ): Step {
        val target = LocalDate.parse(date)
        return StepBuilder("riderSettlementStep", jobRepository)
            .chunk<Long, Long>(CHUNK_SIZE, transactionManager)
            .reader(deliveredRiderIdReader)
            .processor(
                // Kotlin에서 ItemProcessor<Long, Long?>로 바로 SAM 변환하면 O의 타입
                // 상한(Any) 위반으로 컴파일이 안 된다 — 제네릭 인자는 Long(non-null)으로
                // 두고, 오버라이드한 process()의 반환 타입만 Long?로 완화한다(Java
                // 플랫폼 타입 오버라이드는 허용됨). null을 돌려주면 그 아이템은 걸러진다.
                object : ItemProcessor<Long, Long> {
                    override fun process(item: Long): Long? =
                        try {
                            riderSettlementService.calculateRiderSettlement(item, target)
                            item
                        } catch (e: BusinessException) {
                            // 같은 배치가 재실행돼 이미 그날 정산이 있으면 그 라이더만
                            // 건너뛴다(53-5의 DB 유니크 제약이 여기서도 방어선이 된다).
                            if (e.errorCode == SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS) null else throw e
                        }
                }
            )
            .writer(ItemWriter { })
            .build()
    }
}
