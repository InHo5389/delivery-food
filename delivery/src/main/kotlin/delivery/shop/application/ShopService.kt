package delivery.shop.application

import delivery.common.exception.BusinessException
import delivery.shop.application.dto.CreateShopCommand
import delivery.shop.application.dto.NearbyShopQuery
import delivery.shop.application.dto.NearbyShopResult
import delivery.shop.application.dto.UpdateShopCommand
import delivery.shop.domain.Shop
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.ShopRepository
import delivery.shop.infrastructure.ShopSearchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ShopService(
    private val shopRepository: ShopRepository,
    private val shopSearchRepository: ShopSearchRepository,
) {
    @Transactional
    fun create(command: CreateShopCommand): Shop =
        shopRepository.save(
            Shop(
                ownerId = command.ownerId,
                name = command.name,
                address = command.address,
                latitude = command.latitude,
                longitude = command.longitude,
                phone = command.phone,
            )
        )

    fun getById(shopId: Long): Shop =
        shopRepository.findById(shopId).orElseThrow { BusinessException(ShopErrorCode.SHOP_NOT_FOUND) }

    fun getAllByOwnerId(ownerId: Long): List<Shop> = shopRepository.findAllByOwnerId(ownerId)

    fun getNearbyOpenShops(query: NearbyShopQuery): List<NearbyShopResult> =
        shopSearchRepository.findNearbyOpenShops(query.latitude, query.longitude, query.limit, query.offset)
            .map { NearbyShopResult(it.id, it.name, it.address, it.distanceMeters) }

    @Transactional
    fun update(shopId: Long, command: UpdateShopCommand): Shop {
        val shop = getById(shopId)
        shop.name = command.name
        shop.address = command.address
        shop.latitude = command.latitude
        shop.longitude = command.longitude
        shop.phone = command.phone
        return shop
    }

    @Transactional
    fun open(shopId: Long) {
        getById(shopId).open()
    }

    @Transactional
    fun close(shopId: Long) {
        getById(shopId).close()
    }

    @Transactional
    fun delete(shopId: Long) {
        shopRepository.delete(getById(shopId))
    }
}
