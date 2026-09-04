package delivery.notification.api.dto

import delivery.notification.domain.Notification
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val orderId: Long,
    val message: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(notification.id!!, notification.orderId, notification.message, notification.createdAt)
    }
}

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
)
