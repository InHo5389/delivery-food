package delivery.order.domain

import delivery.common.exception.BusinessException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// order_id는 같은 order 모듈 내 orders 테이블을 가리키지만, 아직 OrderRepository(저장 계층)가
// 없는 시점이라 FK는 걸지 않았다(V11 마이그레이션 참조). 주문 1건당 결제는 1건이라
// order_id에 유니크 제약을 둔다.
@Entity
@Table(name = "payment")
class Payment(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(nullable = false)
    val amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.READY,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun transitionTo(next: PaymentStatus) {
        if (!status.canTransitionTo(next)) {
            throw BusinessException(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION)
        }
        status = next
        updatedAt = Instant.now()
    }

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            orderId: Long,
            amount: Long,
            status: PaymentStatus = PaymentStatus.READY,
        ): Payment = Payment(orderId, amount, status).also { it.id = id }
    }
}
