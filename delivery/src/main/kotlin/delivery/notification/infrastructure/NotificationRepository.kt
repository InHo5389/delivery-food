package delivery.notification.infrastructure

import delivery.notification.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    // "내 알림 목록" 조회 화면에서 최신순으로 쓴다.
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>
}
