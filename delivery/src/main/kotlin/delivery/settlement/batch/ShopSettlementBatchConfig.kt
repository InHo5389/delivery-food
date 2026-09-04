package delivery.settlement.batch

import delivery.common.exception.BusinessException
import delivery.settlement.application.ShopSettlementService
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

// ⚠️ 의도적 구식 구현 — RiderSettlementBatchConfig와 동일한 이유(53-6 참조).
@Configuration
class ShopSettlementBatchConfig(
    private val shopSettlementService: ShopSettlementService,
) {
    @Bean
    fun shopSettlementJob(jobRepository: JobRepository, shopSettlementStep: Step): Job =
        JobBuilder("shopSettlementJob", jobRepository)
            .start(shopSettlementStep)
            .build()

    @Bean
    @JobScope
    fun shopSettlementStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        deliveredShopIdReader: ItemReader<Long>,
        @Value("#{jobParameters['weekStart']}") weekStart: String,
    ): Step {
        val monday = LocalDate.parse(weekStart)
        return StepBuilder("shopSettlementStep", jobRepository)
            .chunk<Long, Long>(CHUNK_SIZE, transactionManager)
            .reader(deliveredShopIdReader)
            .processor(
                // RiderSettlementBatchConfig와 같은 이유로 익명 객체를 쓴다(Kotlin에서
                // ItemProcessor<Long, Long?> SAM 변환은 타입 상한 위반으로 컴파일 불가).
                object : ItemProcessor<Long, Long> {
                    override fun process(item: Long): Long? =
                        try {
                            shopSettlementService.calculateShopSettlement(item, monday)
                            item
                        } catch (e: BusinessException) {
                            if (e.errorCode == SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS) null else throw e
                        }
                }
            )
            .writer(ItemWriter { })
            .build()
    }
}
