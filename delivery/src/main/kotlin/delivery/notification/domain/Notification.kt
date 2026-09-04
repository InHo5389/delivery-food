package delivery.notification.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// orderId는 order 모듈 테이블에 대한 논리 참조라 FK를 걸지 않는다(모듈 간 FK 금지).
// message는 발송 시점에 완성된 문자열을 그대로 저장한다(템플릿+파라미터로 나누지 않음) —
// 이 프로젝트에서 알림 문구가 늘어날 다국어/개인화 요구가 없어 지금 분리는 과설계다.
@Entity
@Table(name = "notification")
class Notification(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(nullable = false)
    val message: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        // 테스트에서 저장 후 상태(id 채번 완료)를 흉내내기 위한 팩토리. 프로덕션 코드에서는 사용하지 않는다.
        fun withId(id: Long, userId: Long, orderId: Long, message: String, createdAt: Instant = Instant.now()): Notification =
            Notification(userId, orderId, message, createdAt).also { it.id = id }
    }
}
