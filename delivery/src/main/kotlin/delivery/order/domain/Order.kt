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

// shop_id는 다른 모듈(shop) 테이블을 가리키는 논리 참조다. 모듈 경계 규칙(1-8절)에 따라
// FK를 걸지 않는다 — order와 shop이 각자 별도 DB로 분리(Phase 5)될 수 있어야 하기 때문이다.
//
// customerName/customerPhone은 주문 생성 시점의 값을 그대로 복사해둔 스냅샷이다.
// 이후 고객이 이름을 바꿔도 이미 생성된 주문 레코드는 영향을 받지 않는다. 이 스냅샷
// 덕분에 order는 조회 시 auth를 다시 호출할 필요가 없어 팬인(fan-in) 0을 유지한다.
//
// deliveryFee도 같은 이유의 스냅샷이다 — shop.delivery_fee는 상점의 "현재" 설정값이라,
// 나중에 상점이 배달비를 바꾸면 과거 주문의 실제 청구액을 더 이상 알 수 없게 된다.
// 이 값은 결제 금액(Payment.amount = 메뉴 합계 + deliveryFee)에 실제로 반영되고,
// 라이더 정산(53-3)이 "이 배달로 얼마를 지급해야 하는지" 판단하는 근거가 된다.
//
// 장바구니 항목(메뉴 여러 개)은 주문 1건(Order) + 여러 항목(OrderItem)으로 모델링한다.
// 결제(Payment)도 주문 1건 단위로 묶이므로, 취소/환불을 항목별이 아니라 주문 전체
// 단위로 정확히 처리할 수 있다(커밋 39).
@Entity
@Table(name = "orders")
class Order(
    @Column(name = "customer_id", nullable = false)
    val customerId: Long,

    @Column(name = "shop_id", nullable = false)
    val shopId: Long,

    @Column(name = "customer_name", nullable = false)
    val customerName: String,

    @Column(name = "customer_phone", nullable = false)
    val customerPhone: String,

    @Column(name = "delivery_fee", nullable = false)
    val deliveryFee: Long = 0L,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.CREATED,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun transitionTo(next: OrderStatus) {
        if (!status.canTransitionTo(next)) {
            throw BusinessException(OrderErrorCode.INVALID_ORDER_STATUS_TRANSITION)
        }
        status = next
        updatedAt = Instant.now()
    }

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            customerId: Long,
            shopId: Long,
            customerName: String,
            customerPhone: String,
            deliveryFee: Long = 0L,
            status: OrderStatus = OrderStatus.CREATED,
        ): Order = Order(customerId, shopId, customerName, customerPhone, deliveryFee, status).also { it.id = id }
    }
}
