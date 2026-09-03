package delivery.shop.domain

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

// order_id는 다른 모듈(order) 테이블에 대한 논리 참조다. 모듈 경계 규칙에 따라 FK를
// 걸지 않는다. customerName/totalAmount는 주문 생성 시점 스냅샷이라 조회 시 order
// 모듈을 다시 호출할 필요가 없다(팬인 0, Order의 스냅샷 설계와 동일한 이유).
@Entity
@Table(name = "order_ticket")
class OrderTicket(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "shop_id", nullable = false)
    val shopId: Long,

    @Column(name = "customer_name", nullable = false)
    val customerName: String,

    @Column(name = "total_amount", nullable = false)
    val totalAmount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderTicketStatus = OrderTicketStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun transitionTo(next: OrderTicketStatus) {
        if (!status.canTransitionTo(next)) {
            throw BusinessException(OrderTicketErrorCode.INVALID_ORDER_TICKET_STATUS_TRANSITION)
        }
        status = next
        updatedAt = Instant.now()
    }

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            orderId: Long,
            shopId: Long,
            customerName: String = "홍길동",
            totalAmount: Long = 0,
            status: OrderTicketStatus = OrderTicketStatus.PENDING,
        ): OrderTicket = OrderTicket(orderId, shopId, customerName, totalAmount, status).also { it.id = id }
    }
}
