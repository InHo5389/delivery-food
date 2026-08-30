package delivery.shop.application

import delivery.common.exception.BusinessException
import delivery.shop.application.dto.CreateMenuCommand
import delivery.shop.application.dto.CreateMenuGroupCommand
import delivery.shop.application.dto.UpdateMenuCommand
import delivery.shop.domain.Menu
import delivery.shop.domain.MenuGroup
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.MenuGroupRepository
import delivery.shop.infrastructure.MenuRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MenuService(
    private val menuGroupRepository: MenuGroupRepository,
    private val menuRepository: MenuRepository,
) {
    @Transactional
    fun createMenuGroup(command: CreateMenuGroupCommand): MenuGroup =
        menuGroupRepository.save(
            MenuGroup(
                shopId = command.shopId,
                name = command.name,
                displayOrder = command.displayOrder,
            )
        )

    fun getMenuGroupsByShopId(shopId: Long): List<MenuGroup> =
        menuGroupRepository.findAllByShopIdOrderByDisplayOrder(shopId)

    @Transactional
    fun createMenu(command: CreateMenuCommand): Menu =
        menuRepository.save(
            Menu(
                shopId = command.shopId,
                menuGroupId = command.menuGroupId,
                name = command.name,
                description = command.description,
                price = command.price,
                displayOrder = command.displayOrder,
            )
        )

    fun getMenuById(menuId: Long): Menu =
        menuRepository.findById(menuId).orElseThrow { BusinessException(ShopErrorCode.MENU_NOT_FOUND) }

    fun getMenusByShopId(shopId: Long): List<Menu> = menuRepository.findAllByShopIdOrderByDisplayOrder(shopId)

    fun getMenusByMenuGroupId(menuGroupId: Long): List<Menu> =
        menuRepository.findAllByMenuGroupIdOrderByDisplayOrder(menuGroupId)

    @Transactional
    fun update(menuId: Long, command: UpdateMenuCommand): Menu {
        val menu = getMenuById(menuId)
        menu.name = command.name
        menu.description = command.description
        menu.price = command.price
        menu.displayOrder = command.displayOrder
        return menu
    }

    @Transactional
    fun markSoldOut(menuId: Long) {
        getMenuById(menuId).soldOut = true
    }

    @Transactional
    fun markInStock(menuId: Long) {
        getMenuById(menuId).soldOut = false
    }

    @Transactional
    fun delete(menuId: Long) {
        menuRepository.delete(getMenuById(menuId))
    }
}
