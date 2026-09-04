package delivery.coupon.infrastructure.batch

import delivery.coupon.domain.IssuanceStatus
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

// ⚠️ 의도적 구식 구현(04_Phase1_기본틀.md 53-11 참조) — OFFSET 기반 페이징(JpaPagingItemReader
// 기본 동작)으로 대상을 순회한다. Phase 3에서 정산 배치와 함께 Reader 개선 대상에 포함될 수 있다.
//
// 이 쿼리는 자신이 필터링하는 조건(status=ISSUED)을 프로세서가 처리 중에 직접 바꾼다
// (Issuance.expire()가 상태를 EXPIRED로 바꿈) — 그래서 한 페이지를 처리해 그 페이지의
// 행들이 조건을 더 이상 만족하지 않게 되면, 다음 페이지의 OFFSET이 전체 결과집합에서
// 밀려나 아직 처리 못한 뒷부분 일부를 이번 실행에서 건너뛸 수 있다. 다만 사용 가능
// 여부는 CouponService.use()가 status가 아니라 expiresAt을 직접 비교해 판단하므로
// 실제 사용 가능 여부에는 영향이 없고, 건너뛴 행은 다음날 배치가 다시 골라내 처리한다
// (하루 지연되는 하우스키핑 지연일 뿐, 만료된 쿠폰이 실제로 쓰일 수 있게 되는 건 아니다).
private const val PAGE_SIZE = 100

@Configuration
class ExpiredIssuanceIdReaderConfig {
    // 잡 파라미터(asOf)는 잡 실행 시점에만 알 수 있어 @StepScope로 늦게 바인딩한다.
    @Bean
    @StepScope
    fun expiredIssuanceIdReader(
        entityManagerFactory: EntityManagerFactory,
        @Value("#{jobParameters['asOf']}") asOf: String,
    ): ItemReader<Long> =
        JpaPagingItemReaderBuilder<Long>()
            .name("expiredIssuanceIdReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString(
                """
                SELECT i.id FROM Issuance i
                WHERE i.status = :status AND i.expiresAt < :asOf
                ORDER BY i.expiresAt
                """.trimIndent()
            )
            .parameterValues(mapOf("status" to IssuanceStatus.ISSUED, "asOf" to Instant.parse(asOf)))
            .pageSize(PAGE_SIZE)
            .build()
}
