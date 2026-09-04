package delivery.settlement.infrastructure

import delivery.settlement.domain.SettlementItem
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementItemRepository : JpaRepository<SettlementItem, Long> {
    fun findAllBySettlementId(settlementId: Long): List<SettlementItem>
}
