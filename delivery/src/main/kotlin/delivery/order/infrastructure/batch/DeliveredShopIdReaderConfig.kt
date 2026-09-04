package delivery.order.infrastructure.batch

import delivery.order.domain.OrderStatus
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

// settlement 모듈의 상점 정산 배치(53-6)가 쓰는 리더다 — Order는 order 모듈이 소유한
// 엔티티라, 이 리더를 order 모듈 안에 두고 settlement은 "Long을 돌려주는 ItemReader"
// 빈만 주입받아 쓴다(모듈 경계 규칙 2·4절 참조, DeliveredRiderIdReaderConfig와 동일한 이유).
private val KST = ZoneId.of("Asia/Seoul")
private const val PAGE_SIZE = 100

@Configuration
class DeliveredShopIdReaderConfig {
    // 잡 파라미터(weekStart)는 잡 실행 시점에만 알 수 있어 @StepScope로 늦게 바인딩한다.
    @Bean
    @StepScope
    fun deliveredShopIdReader(
        entityManagerFactory: EntityManagerFactory,
        @Value("#{jobParameters['weekStart']}") weekStart: String,
    ): ItemReader<Long> {
        val monday = LocalDate.parse(weekStart)
        val from: Instant = monday.atStartOfDay(KST).toInstant()
        val to: Instant = monday.plusWeeks(1).atStartOfDay(KST).toInstant()

        return JpaPagingItemReaderBuilder<Long>()
            .name("deliveredShopIdReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString(
                """
                SELECT DISTINCT o.shopId FROM Order o
                WHERE o.status = :status AND o.updatedAt >= :from AND o.updatedAt < :to
                ORDER BY o.shopId
                """.trimIndent()
            )
            .parameterValues(mapOf("status" to OrderStatus.DELIVERED, "from" to from, "to" to to))
            .pageSize(PAGE_SIZE)
            .build()
    }
}
