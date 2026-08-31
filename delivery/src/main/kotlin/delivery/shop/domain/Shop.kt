package delivery.shop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "shop")
class Shop(
    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var address: String,

    @Column(nullable = false)
    var latitude: BigDecimal,

    @Column(nullable = false)
    var longitude: BigDecimal,

    @Column(nullable = false)
    var phone: String,

    @Column(name = "min_order_amount", nullable = false)
    var minOrderAmount: Long = 0,

    @Column(name = "delivery_fee", nullable = false)
    var deliveryFee: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ShopStatus = ShopStatus.CLOSED,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun open() {
        status = ShopStatus.OPEN
        updatedAt = Instant.now()
    }

    fun close() {
        status = ShopStatus.CLOSED
        updatedAt = Instant.now()
    }

    fun isOpen(): Boolean = status == ShopStatus.OPEN

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(
            id: Long,
            ownerId: Long,
            name: String,
            address: String,
            phone: String,
            latitude: BigDecimal = BigDecimal("37.5665000"),
            longitude: BigDecimal = BigDecimal("126.9780000"),
            status: ShopStatus = ShopStatus.CLOSED,
            minOrderAmount: Long = 0,
            deliveryFee: Long = 0,
        ): Shop = Shop(ownerId, name, address, latitude, longitude, phone, minOrderAmount, deliveryFee, status).also { it.id = id }
    }
}
