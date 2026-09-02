package delivery.order.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.random.Random

// 실제 PG사 연동은 학습 범위 밖이다. 실패율을 설정으로 주입해
// Phase 3~4에서 재시도(Backoff+Jitter), DLQ, 장애 주입 테스트에 그대로 활용한다.
@Component
class MockPgClient(
    @Value("\${payment.mock-pg.failure-rate:0.0}") private val failureRate: Double,
) {
    fun authorize(orderId: Long, amount: Long): Boolean {
        require(amount > 0) { "결제 금액은 0보다 커야 합니다." }
        return Random.nextDouble() >= failureRate
    }

    fun refund(orderId: Long, amount: Long): Boolean {
        require(amount > 0) { "환불 금액은 0보다 커야 합니다." }
        return Random.nextDouble() >= failureRate
    }
}
