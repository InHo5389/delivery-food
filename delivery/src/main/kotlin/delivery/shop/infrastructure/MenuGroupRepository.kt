package delivery.shop.infrastructure

import delivery.shop.domain.MenuGroup
import org.springframework.data.jpa.repository.JpaRepository

interface MenuGroupRepository : JpaRepository<MenuGroup, Long> {
    fun findAllByShopIdOrderByDisplayOrder(shopId: Long): List<MenuGroup>
}
