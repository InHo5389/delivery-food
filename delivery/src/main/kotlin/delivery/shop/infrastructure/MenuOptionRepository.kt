package delivery.shop.infrastructure

import delivery.shop.domain.MenuOption
import org.springframework.data.jpa.repository.JpaRepository

interface MenuOptionRepository : JpaRepository<MenuOption, Long> {
    fun findAllByMenuOptionGroupIdOrderByDisplayOrder(menuOptionGroupId: Long): List<MenuOption>
}
