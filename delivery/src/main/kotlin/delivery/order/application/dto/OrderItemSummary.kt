package delivery.order.application.dto

// 다른 모듈(shop)이 주문 항목을 조회할 때 쓰는 DTO. 엔티티(OrderItem)를 모듈 경계
// 밖으로 그대로 내보내지 않기 위해 필요한 필드만 담은 별도 타입으로 둔다.
data class OrderItemSummary(
    val menuName: String,
    val menuPrice: Long,
    val quantity: Int,
)
