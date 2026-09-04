package delivery.coupon.batch

import delivery.common.exception.BusinessException
import delivery.coupon.application.CouponService
import delivery.coupon.domain.CouponErrorCode
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

private const val CHUNK_SIZE = 100

// expireIssuance() 자체가 이미 Issuance 상태 변경을 커밋하는 완결된 @Transactional
// 단위라서, 이 Step의 ItemWriter는 할 일이 없다(정산 배치들과 동일한 구조 — 커밋 53-6 참조).
@Configuration
class CouponExpiryBatchConfig(
    private val couponService: CouponService,
) {
    @Bean
    fun couponExpiryJob(jobRepository: JobRepository, couponExpiryStep: Step): Job =
        JobBuilder("couponExpiryJob", jobRepository)
            .start(couponExpiryStep)
            .build()

    @Bean
    @JobScope
    fun couponExpiryStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        expiredIssuanceIdReader: ItemReader<Long>,
    ): Step =
        StepBuilder("couponExpiryStep", jobRepository)
            .chunk<Long, Long>(CHUNK_SIZE, transactionManager)
            .reader(expiredIssuanceIdReader)
            .processor(
                // Kotlin 제네릭/SAM 변환 제약(53-6 참조)으로 object 표현식을 쓴다.
                object : ItemProcessor<Long, Long> {
                    override fun process(item: Long): Long? =
                        try {
                            couponService.expireIssuance(item)
                            item
                        } catch (e: BusinessException) {
                            // reader가 골라온 뒤 사용자가 먼저 사용해버려 상태가 이미
                            // USED로 바뀐 경우 — 그 건만 건너뛴다.
                            if (e.errorCode == CouponErrorCode.INVALID_ISSUANCE_STATUS_TRANSITION) null else throw e
                        }
                }
            )
            .writer(ItemWriter { })
            .build()
}
