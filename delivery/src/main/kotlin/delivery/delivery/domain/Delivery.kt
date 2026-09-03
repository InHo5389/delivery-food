package delivery.delivery.domain

import delivery.common.exception.BusinessException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

// order_id/shop_id는 다른 모듈(order/shop) 테이블에 대한 논리 참조다. 모듈 경계 규칙에 따라
// FK를 걸지 않는다. pickupLatitude/pickupLongitude는 배차 시점 상점 위치의 스냅샷이다 —
// 배차 매칭 엔진이 매 사이클 shop 모듈을 다시 호출하지 않도록(팬인 0) 한다.
@Entity
@Table(name = "delivery")
class Delivery(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "shop_id", nullable = false)
    val shopId: Long,

    @Column(name = "pickup_latitude", nullable = false)
    val pickupLatitude: BigDecimal,

    @Column(name = "pickup_longitude", nullable = false)
    val pickupLongitude: BigDecimal,

    // 사장님이 주문 접수 시 입력한 조리 예상 시간을 기준으로 계산한 값. 라이더가 배차
    // 큐에서 "언제쯤 가면 되는지" 판단하는 근거로 쓴다.
    @Column(name = "estimated_pickup_at")
    val estimatedPickupAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeliveryStatus = DeliveryStatus.PENDING,

    @Column(name = "rider_id")
    var riderId: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun transitionTo(next: DeliveryStatus) {
        if (!status.canTransitionTo(next)) {
            throw BusinessException(DeliveryErrorCode.INVALID_DELIVERY_STATUS_TRANSITION)
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
            pickupLatitude: BigDecimal = BigDecimal("37.5665000"),
            pickupLongitude: BigDecimal = BigDecimal("126.9780000"),
            estimatedPickupAt: Instant? = null,
            status: DeliveryStatus = DeliveryStatus.PENDING,
        ): Delivery = Delivery(
            orderId = orderId,
            shopId = shopId,
            pickupLatitude = pickupLatitude,
            pickupLongitude = pickupLongitude,
            estimatedPickupAt = estimatedPickupAt,
            status = status,
        ).also { it.id = id }
    }
}
