package delivery.shop.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.shop.application.dto.BulkCreateMenuCommand
import delivery.shop.application.dto.CreateMenuCommand
import delivery.shop.application.dto.CreateMenuGroupCommand
import delivery.shop.application.dto.UpdateMenuCommand
import delivery.shop.domain.Menu
import delivery.shop.domain.MenuGroup
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.MenuGroupRepository
import delivery.shop.infrastructure.MenuImageStorage
import delivery.shop.infrastructure.MenuRepository
import delivery.shop.infrastructure.ShopRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

@Service
class MenuService(
    private val menuGroupRepository: MenuGroupRepository,
    private val menuRepository: MenuRepository,
    private val menuImageStorage: MenuImageStorage,
    private val shopRepository: ShopRepository,
) {
    @Transactional
    fun createMenuGroup(command: CreateMenuGroupCommand, requester: AuthenticatedUser): MenuGroup {
        assertShopOwner(command.shopId, requester)
        return menuGroupRepository.save(
            MenuGroup(
                shopId = command.shopId,
                name = command.name,
                displayOrder = command.displayOrder,
            )
        )
    }

    fun getMenuGroupsByShopId(shopId: Long): List<MenuGroup> =
        menuGroupRepository.findAllByShopIdOrderByDisplayOrder(shopId)

    @Transactional
    fun createMenu(command: CreateMenuCommand, requester: AuthenticatedUser): Menu {
        assertShopOwner(command.shopId, requester)
        val menuGroup = menuGroupRepository.findById(command.menuGroupId)
            .orElseThrow { BusinessException(ShopErrorCode.MENU_GROUP_NOT_FOUND) }
        if (menuGroup.shopId != command.shopId) {
            throw BusinessException(ShopErrorCode.MENU_GROUP_NOT_FOUND)
        }
        return menuRepository.save(
            Menu(
                shopId = command.shopId,
                menuGroupId = command.menuGroupId,
                name = command.name,
                description = command.description,
                price = command.price,
                displayOrder = command.displayOrder,
            )
        )
    }

    @Transactional
    fun createMenus(command: BulkCreateMenuCommand, requester: AuthenticatedUser): List<Menu> {
        if (command.menus.isEmpty()) {
            throw BusinessException(ShopErrorCode.EMPTY_MENU_LIST)
        }
        assertShopOwner(command.shopId, requester)
        val menuGroupIds = command.menus.map { it.menuGroupId }.distinct()
        val menuGroups = menuGroupRepository.findAllById(menuGroupIds).associateBy { it.id }
        menuGroupIds.forEach { menuGroupId ->
            val menuGroup = menuGroups[menuGroupId] ?: throw BusinessException(ShopErrorCode.MENU_GROUP_NOT_FOUND)
            if (menuGroup.shopId != command.shopId) {
                throw BusinessException(ShopErrorCode.MENU_GROUP_NOT_FOUND)
            }
        }
        val menus = command.menus.map { item ->
            Menu(
                shopId = command.shopId,
                menuGroupId = item.menuGroupId,
                name = item.name,
                description = item.description,
                price = item.price,
                displayOrder = item.displayOrder,
            )
        }
        return menuRepository.saveAll(menus)
    }

    fun getMenuById(menuId: Long): Menu =
        menuRepository.findById(menuId).orElseThrow { BusinessException(ShopErrorCode.MENU_NOT_FOUND) }

    fun getMenusByShopId(shopId: Long): List<Menu> = menuRepository.findAllByShopIdOrderByDisplayOrder(shopId)

    fun getMenusByMenuGroupId(menuGroupId: Long): List<Menu> =
        menuRepository.findAllByMenuGroupIdOrderByDisplayOrder(menuGroupId)

    @Transactional
    fun update(menuId: Long, command: UpdateMenuCommand, requester: AuthenticatedUser): Menu {
        val menu = getMenuById(menuId)
        assertShopOwner(menu.shopId, requester)
        menu.name = command.name
        menu.description = command.description
        menu.price = command.price
        menu.displayOrder = command.displayOrder
        return menu
    }

    @Transactional
    fun markSoldOut(menuId: Long, requester: AuthenticatedUser) {
        val menu = getMenuById(menuId)
        assertShopOwner(menu.shopId, requester)
        menu.soldOut = true
    }

    @Transactional
    fun markInStock(menuId: Long, requester: AuthenticatedUser) {
        val menu = getMenuById(menuId)
        assertShopOwner(menu.shopId, requester)
        menu.soldOut = false
    }

    @Transactional
    fun delete(menuId: Long, requester: AuthenticatedUser) {
        val menu = getMenuById(menuId)
        assertShopOwner(menu.shopId, requester)
        menuRepository.delete(menu)
    }

    @Transactional
    fun uploadImage(menuId: Long, file: MultipartFile, requester: AuthenticatedUser): Menu {
        val extension = file.originalFilename?.substringAfterLast('.', "")?.lowercase().orEmpty()
        if (file.isEmpty || extension !in SUPPORTED_IMAGE_EXTENSIONS) {
            throw BusinessException(ShopErrorCode.INVALID_MENU_IMAGE)
        }
        val menu = getMenuById(menuId)
        assertShopOwner(menu.shopId, requester)
        val filename = menuImageStorage.store(file)
        menu.imageUrl = filename
        return menu
    }

    // ★ 지금은 이 서비스가 직접 검증하지만, Phase 5에서 게이트웨이가 X-User-Id/X-Role을
    //   주입해줘도 이 검증 로직(요청자==상점 사장님)은 shop 도메인 규칙이라 그대로 남는다.
    private fun assertShopOwner(shopId: Long, requester: AuthenticatedUser) {
        val shop = shopRepository.findById(shopId).orElseThrow { BusinessException(ShopErrorCode.SHOP_NOT_FOUND) }
        if (requester.role != Role.OWNER || shop.ownerId != requester.userId) {
            throw BusinessException(ShopErrorCode.NOT_SHOP_OWNER)
        }
    }

    fun getImagePath(menuId: Long): java.nio.file.Path {
        val menu = getMenuById(menuId)
        val filename = menu.imageUrl ?: throw BusinessException(ShopErrorCode.MENU_IMAGE_NOT_FOUND)
        if (!menuImageStorage.exists(filename)) {
            throw BusinessException(ShopErrorCode.MENU_IMAGE_NOT_FOUND)
        }
        return menuImageStorage.resolve(filename)
    }
}
