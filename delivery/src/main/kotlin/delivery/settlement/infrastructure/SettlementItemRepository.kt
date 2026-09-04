package delivery.settlement.infrastructure

import delivery.settlement.domain.SettlementItem
import delivery.settlement.domain.SettlementItemType
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementItemRepository : JpaRepository<SettlementItem, Long> {
    fun findAllBySettlementId(settlementId: Long): List<SettlementItem>

    // 환불 항목의 요율을 정할 때, 같은 주문의 과거 판매(SALE) 항목이 있으면 그때 적용됐던
    // 요율을 그대로 재사용하기 위한 조회다(요율이 바뀌어도 과거 계산이 흔들리지 않도록).
    fun findTopByOrderIdAndTypeOrderByCreatedAtDesc(orderId: Long, type: SettlementItemType): SettlementItem?
}
