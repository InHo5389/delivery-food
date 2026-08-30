package delivery.shop.application

import delivery.common.exception.BusinessException
import delivery.shop.application.dto.CreateOwnerProfileCommand
import delivery.shop.application.dto.UpdateOwnerProfileCommand
import delivery.shop.domain.OwnerProfile
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.OwnerProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OwnerProfileService(
    private val ownerProfileRepository: OwnerProfileRepository,
) {
    @Transactional
    fun create(command: CreateOwnerProfileCommand): OwnerProfile {
        if (ownerProfileRepository.existsByAccountId(command.accountId)) {
            throw BusinessException(ShopErrorCode.OWNER_PROFILE_ALREADY_EXISTS)
        }
        return ownerProfileRepository.save(
            OwnerProfile(
                accountId = command.accountId,
                businessRegistrationNumber = command.businessRegistrationNumber,
                businessName = command.businessName,
                settlementBank = command.settlementBank,
                settlementAccount = command.settlementAccount,
            )
        )
    }

    fun getByAccountId(accountId: Long): OwnerProfile =
        ownerProfileRepository.findByAccountId(accountId)
            ?: throw BusinessException(ShopErrorCode.OWNER_PROFILE_NOT_FOUND)

    @Transactional
    fun update(accountId: Long, command: UpdateOwnerProfileCommand): OwnerProfile {
        val profile = getByAccountId(accountId)
        profile.businessRegistrationNumber = command.businessRegistrationNumber
        profile.businessName = command.businessName
        profile.settlementBank = command.settlementBank
        profile.settlementAccount = command.settlementAccount
        return profile
    }
}
