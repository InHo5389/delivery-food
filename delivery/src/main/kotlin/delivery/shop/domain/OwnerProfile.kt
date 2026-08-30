package delivery.shop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "owner_profile")
class OwnerProfile(
    @Column(name = "account_id", nullable = false)
    val accountId: Long,

    @Column(name = "business_registration_number", nullable = false)
    var businessRegistrationNumber: String,

    @Column(name = "business_name", nullable = false)
    var businessName: String,

    @Column(name = "settlement_bank", nullable = false)
    var settlementBank: String,

    @Column(name = "settlement_account", nullable = false)
    var settlementAccount: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun withId(
            id: Long,
            accountId: Long,
            businessRegistrationNumber: String,
            businessName: String,
            settlementBank: String,
            settlementAccount: String,
        ): OwnerProfile =
            OwnerProfile(accountId, businessRegistrationNumber, businessName, settlementBank, settlementAccount)
                .also { it.id = id }
    }
}
