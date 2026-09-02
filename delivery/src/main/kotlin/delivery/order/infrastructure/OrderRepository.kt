package delivery.order.infrastructure

import delivery.order.domain.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    // ⚠️ 의도적 구식 구현 — Phase 3 A-3에서 커서 기반(WHERE id < :cursor)으로 개선 예정.
    //   Pageable이 내부적으로 LIMIT ? OFFSET ?을 생성한다. 페이지가 깊어질수록
    //   OFFSET만큼의 행을 읽고 버리므로 뒤 페이지에서 선형적으로 느려진다.
    fun findAllByCustomerIdOrderByIdDesc(customerId: Long, pageable: Pageable): Page<Order>
}
