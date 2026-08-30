package delivery.shop.infrastructure

import delivery.shop.domain.Menu
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRepository : JpaRepository<Menu, Long> {
    fun findAllByShopIdOrderByDisplayOrder(shopId: Long): List<Menu>
    fun findAllByMenuGroupIdOrderByDisplayOrder(menuGroupId: Long): List<Menu>
}
