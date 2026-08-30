package delivery.shop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

@Entity
@Table(name = "business_hours")
class BusinessHours(
    @Column(name = "shop_id", nullable = false)
    val shopId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: DayOfWeek,

    @Column(name = "open_time", nullable = false)
    var openTime: LocalTime,

    @Column(name = "close_time", nullable = false)
    var closeTime: LocalTime,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    init {
        require(openTime != closeTime) { "영업 시작 시각과 종료 시각은 같을 수 없습니다." }
    }

    // closeTime이 openTime보다 이르면(예: 22:00~02:00) 자정을 넘겨 다음 날 새벽까지 영업하는 것으로 본다.
    private val crossesMidnight: Boolean
        get() = closeTime.isBefore(openTime)

    fun isWithin(time: LocalTime): Boolean =
        if (crossesMidnight) {
            !time.isBefore(openTime) || time.isBefore(closeTime)
        } else {
            !time.isBefore(openTime) && time.isBefore(closeTime)
        }

    companion object {
        fun withId(id: Long, shopId: Long, dayOfWeek: DayOfWeek, openTime: LocalTime, closeTime: LocalTime): BusinessHours =
            BusinessHours(shopId, dayOfWeek, openTime, closeTime).also { it.id = id }
    }
}
