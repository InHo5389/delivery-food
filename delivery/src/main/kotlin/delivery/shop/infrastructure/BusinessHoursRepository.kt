package delivery.shop.infrastructure

import delivery.shop.domain.BusinessHours
import org.springframework.data.jpa.repository.JpaRepository

interface BusinessHoursRepository : JpaRepository<BusinessHours, Long> {
    fun findAllByShopId(shopId: Long): List<BusinessHours>
}
