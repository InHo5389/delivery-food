package delivery.shop.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.shop.application.dto.CreateShopCommand
import delivery.shop.application.dto.MenuGroupResult
import delivery.shop.application.dto.NearbyShopQuery
import delivery.shop.application.dto.NearbyShopResult
import delivery.shop.application.dto.ShopDetailResult
import delivery.shop.application.dto.UpdateShopCommand
import delivery.shop.domain.Shop
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.BusinessHoursRepository
import delivery.shop.infrastructure.ShopRepository
import delivery.shop.infrastructure.ShopSearchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ShopService(
    private val shopRepository: ShopRepository,
    private val shopSearchRepository: ShopSearchRepository,
    private val businessHoursRepository: BusinessHoursRepository,
    private val menuService: MenuService,
) {
    @Transactional
    fun create(command: CreateShopCommand, requester: AuthenticatedUser): Shop {
        if (requester.role != Role.OWNER) {
            throw BusinessException(ShopErrorCode.NOT_SHOP_OWNER)
        }
        return shopRepository.save(
            Shop(
                ownerId = requester.userId,
                name = command.name,
                address = command.address,
                latitude = command.latitude,
                longitude = command.longitude,
                phone = command.phone,
                minOrderAmount = command.minOrderAmount,
                deliveryFee = command.deliveryFee,
            )
        )
    }

    fun getById(shopId: Long): Shop =
        shopRepository.findById(shopId).orElseThrow { BusinessException(ShopErrorCode.SHOP_NOT_FOUND) }

    fun getAllByOwnerId(ownerId: Long): List<Shop> = shopRepository.findAllByOwnerId(ownerId)

    // ⚠️ 의도적 구식 구현 — Phase 3 A-5에서 Redis Cache-Aside로 개선 예정.
    //   조회할 때마다 매번 DB에서 상점/영업시간/메뉴그룹/메뉴를 다시 읽는다.
    fun getShopDetail(shopId: Long): ShopDetailResult {
        val shop = getById(shopId)
        val businessHours = businessHoursRepository.findAllByShopId(shopId)
        val menuGroups = menuService.getMenuGroupsByShopId(shopId)
        val menuGroupResults = menuGroups.map { menuGroup ->
            MenuGroupResult(menuGroup, menuService.getMenusByMenuGroupId(menuGroup.id!!))
        }
        return ShopDetailResult(shop, businessHours, menuGroupResults)
    }

    fun getNearbyOpenShops(query: NearbyShopQuery): List<NearbyShopResult> =
        shopSearchRepository.findNearbyOpenShops(query.latitude, query.longitude, query.limit, query.offset)
            .map { NearbyShopResult(it.id, it.name, it.address, it.minOrderAmount, it.deliveryFee, it.distanceMeters) }

    @Transactional
    fun update(shopId: Long, command: UpdateShopCommand, requester: AuthenticatedUser): Shop {
        val shop = getById(shopId)
        assertOwner(shop, requester)
        shop.name = command.name
        shop.address = command.address
        shop.latitude = command.latitude
        shop.longitude = command.longitude
        shop.phone = command.phone
        shop.minOrderAmount = command.minOrderAmount
        shop.deliveryFee = command.deliveryFee
        return shop
    }

    @Transactional
    fun open(shopId: Long, requester: AuthenticatedUser) {
        val shop = getById(shopId)
        assertOwner(shop, requester)
        shop.open()
    }

    @Transactional
    fun close(shopId: Long, requester: AuthenticatedUser) {
        val shop = getById(shopId)
        assertOwner(shop, requester)
        shop.close()
    }

    @Transactional
    fun delete(shopId: Long, requester: AuthenticatedUser) {
        val shop = getById(shopId)
        assertOwner(shop, requester)
        shopRepository.delete(shop)
    }

    private fun assertOwner(shop: Shop, requester: AuthenticatedUser) {
        if (requester.role != Role.OWNER || shop.ownerId != requester.userId) {
            throw BusinessException(ShopErrorCode.NOT_SHOP_OWNER)
        }
    }
}
