package delivery.order.application

import delivery.common.exception.BusinessException
import delivery.order.application.dto.AddCartItemCommand
import delivery.order.application.dto.CartResult
import delivery.order.application.dto.ChangeCartItemQuantityCommand
import delivery.order.domain.Cart
import delivery.order.domain.CartErrorCode
import delivery.order.domain.CartItem
import delivery.order.infrastructure.CartItemRepository
import delivery.order.infrastructure.CartRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
) {
    @Transactional
    fun addItem(command: AddCartItemCommand): CartResult {
        if (command.quantity < 1) {
            throw BusinessException(CartErrorCode.INVALID_CART_ITEM_QUANTITY)
        }

        val cart = cartRepository.findByCustomerId(command.customerId)
            ?: cartRepository.save(Cart(customerId = command.customerId, shopId = command.shopId))

        if (cart.shopId != command.shopId) {
            throw BusinessException(CartErrorCode.DIFFERENT_SHOP_IN_CART)
        }

        val existing = cartItemRepository.findByCartIdAndMenuId(cart.id!!, command.menuId)
        if (existing != null) {
            existing.changeQuantity(existing.quantity + command.quantity)
        } else {
            cartItemRepository.save(
                CartItem(
                    cartId = cart.id!!,
                    menuId = command.menuId,
                    menuName = command.menuName,
                    menuPrice = command.menuPrice,
                    quantity = command.quantity,
                )
            )
        }
        cart.touch()

        return getCart(command.customerId)
    }

    fun getCart(customerId: Long): CartResult {
        val cart = cartRepository.findByCustomerId(customerId)
            ?: throw BusinessException(CartErrorCode.CART_NOT_FOUND)
        val items = cartItemRepository.findAllByCartId(cart.id!!)
        return CartResult(cart, items)
    }

    @Transactional
    fun changeQuantity(command: ChangeCartItemQuantityCommand): CartResult {
        if (command.quantity < 1) {
            throw BusinessException(CartErrorCode.INVALID_CART_ITEM_QUANTITY)
        }

        val cart = cartRepository.findByCustomerId(command.customerId)
            ?: throw BusinessException(CartErrorCode.CART_NOT_FOUND)
        val item = cartItemRepository.findAllByCartId(cart.id!!)
            .find { it.id == command.cartItemId }
            ?: throw BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND)

        item.changeQuantity(command.quantity)
        cart.touch()

        return getCart(command.customerId)
    }

    @Transactional
    fun removeItem(customerId: Long, cartItemId: Long) {
        val cart = cartRepository.findByCustomerId(customerId)
            ?: throw BusinessException(CartErrorCode.CART_NOT_FOUND)
        val item = cartItemRepository.findAllByCartId(cart.id!!)
            .find { it.id == cartItemId }
            ?: throw BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND)

        cartItemRepository.delete(item)
        cart.touch()
    }

    @Transactional
    fun clear(customerId: Long) {
        val cart = cartRepository.findByCustomerId(customerId)
            ?: throw BusinessException(CartErrorCode.CART_NOT_FOUND)
        cartItemRepository.deleteAllByCartId(cart.id!!)
        cart.touch()
    }
}
