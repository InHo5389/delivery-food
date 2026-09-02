package delivery.order.infrastructure

import delivery.order.domain.Cart
import org.springframework.data.jpa.repository.JpaRepository

interface CartRepository : JpaRepository<Cart, Long> {
    fun findByCustomerId(customerId: Long): Cart?
}
