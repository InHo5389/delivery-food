package delivery.notification.api

import delivery.common.security.AuthenticatedUser
import delivery.notification.api.dto.NotificationListResponse
import delivery.notification.api.dto.NotificationResponse
import delivery.notification.application.NotificationService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping("/notifications/subscribe")
    fun subscribe(
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): SseEmitter = notificationService.subscribe(requester.userId)

    @GetMapping("/notifications")
    fun getMyNotifications(
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): NotificationListResponse =
        NotificationListResponse(notificationService.getMyNotifications(requester.userId).map(NotificationResponse::from))
}
