package delivery.shop.infrastructure

import delivery.shop.domain.Shop
import org.springframework.data.jpa.repository.JpaRepository

interface ShopRepository : JpaRepository<Shop, Long> {
    fun findAllByOwnerId(ownerId: Long): List<Shop>
}
