package delivery.shop.infrastructure

import delivery.shop.domain.OwnerProfile
import org.springframework.data.jpa.repository.JpaRepository

interface OwnerProfileRepository : JpaRepository<OwnerProfile, Long> {
    fun findByAccountId(accountId: Long): OwnerProfile?
    fun existsByAccountId(accountId: Long): Boolean
}
