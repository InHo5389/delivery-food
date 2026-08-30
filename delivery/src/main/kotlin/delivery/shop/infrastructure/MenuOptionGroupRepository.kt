package delivery.shop.infrastructure

import delivery.shop.domain.MenuOptionGroup
import org.springframework.data.jpa.repository.JpaRepository

interface MenuOptionGroupRepository : JpaRepository<MenuOptionGroup, Long> {
    fun findAllByMenuIdOrderByDisplayOrder(menuId: Long): List<MenuOptionGroup>
}
