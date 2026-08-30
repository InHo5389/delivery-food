package delivery.shop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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
    var phone: String,

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
            status: ShopStatus = ShopStatus.CLOSED,
        ): Shop = Shop(ownerId, name, address, phone, status).also { it.id = id }
    }
}
