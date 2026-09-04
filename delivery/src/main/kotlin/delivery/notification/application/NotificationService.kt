package delivery.notification.application

import delivery.notification.domain.Notification
import delivery.notification.infrastructure.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

// SSE는 재연결이 자동이지만(브라우저 EventSource 표준 동작), 재연결 사이에 놓친 알림은
// 서버가 다시 밀어주지 않는다 — 그래서 모든 알림을 DB에도 남겨 GET /notifications로
// 놓친 이력을 따로 조회할 수 있게 한다(연결이 끊겨 있던 동안의 알림 유실 대응).
private const val NO_TIMEOUT = 60 * 60 * 1000L // 1시간 - 이 시간이 지나면 브라우저가 자동 재연결한다.

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val emitterRegistry: SseEmitterRegistry,
) {
    fun subscribe(userId: Long): SseEmitter {
        val emitter = SseEmitter(NO_TIMEOUT)
        emitterRegistry.register(userId, emitter)
        try {
            // 연결 직후 더미 이벤트를 하나 보내 프록시가 응답을 버퍼링하지 않고 바로
            // 스트리밍하게 만든다(SSE 관례) — 실패해도 onError 콜백이 정리하므로 무시한다.
            emitter.send(SseEmitter.event().name("connect").data("connected"))
        } catch (e: IOException) {
            // no-op
        }
        return emitter
    }

    @Transactional
    fun notify(userId: Long, orderId: Long, message: String): Notification {
        val notification = notificationRepository.save(Notification(userId, orderId, message))
        emitterRegistry.send(userId, "notification", notification)
        return notification
    }

    fun getMyNotifications(userId: Long): List<Notification> = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
}
