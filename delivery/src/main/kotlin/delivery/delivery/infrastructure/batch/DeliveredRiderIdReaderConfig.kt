package delivery.delivery.infrastructure.batch

import delivery.delivery.domain.DeliveryStatus
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// settlement 모듈의 라이더 정산 배치(53-6)가 쓰는 리더다 — Delivery는 delivery 모듈이
// 소유한 엔티티라, 이 리더를 delivery 모듈 안에 두고 settlement은 "Long을 돌려주는
// ItemReader" 빈만 주입받아 쓴다(모듈 경계 규칙 2·4절 — 다른 모듈 엔티티를 직접
// 참조하지 않고 DTO/원시값만 주고받는다).
private val KST = ZoneId.of("Asia/Seoul")
private const val PAGE_SIZE = 100

@Configuration
class DeliveredRiderIdReaderConfig {
    // 잡 파라미터(date)는 잡 실행 시점에만 알 수 있어 @StepScope로 늦게 바인딩한다.
    @Bean
    @StepScope
    fun deliveredRiderIdReader(
        entityManagerFactory: EntityManagerFactory,
        @Value("#{jobParameters['date']}") date: String,
    ): ItemReader<Long> {
        val target = LocalDate.parse(date)
        val from: Instant = target.atStartOfDay(KST).toInstant()
        val to: Instant = target.plusDays(1).atStartOfDay(KST).toInstant()

        return JpaPagingItemReaderBuilder<Long>()
            .name("deliveredRiderIdReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString(
                """
                SELECT DISTINCT d.riderId FROM Delivery d
                WHERE d.status = :status AND d.updatedAt >= :from AND d.updatedAt < :to
                ORDER BY d.riderId
                """.trimIndent()
            )
            .parameterValues(mapOf("status" to DeliveryStatus.DELIVERED, "from" to from, "to" to to))
            .pageSize(PAGE_SIZE)
            .build()
    }
}
