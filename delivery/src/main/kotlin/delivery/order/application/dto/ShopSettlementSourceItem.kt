package delivery.order.application.dto

// settlement 모듈이 상점 정산을 계산할 때 쓰는 DTO. 판매(DELIVERED 주문 금액)와
// 환불(REFUNDED 결제 금액) 조회 양쪽에서 재사용한다 — 둘 다 "주문 1건당 금액 1개" 모양이 같다.
data class ShopSettlementSourceItem(
    val orderId: Long,
    val amount: Long,
)
