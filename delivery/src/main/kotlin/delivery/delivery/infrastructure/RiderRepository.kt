package delivery.delivery.infrastructure

import delivery.delivery.domain.Rider
import org.springframework.data.jpa.repository.JpaRepository

interface RiderRepository : JpaRepository<Rider, Long>
