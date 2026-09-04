package delivery.notification.application

import delivery.notification.domain.Notification
import delivery.notification.infrastructure.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NotificationServiceTest {

    private val notificationRepository = mockk<NotificationRepository>()
    private val emitterRegistry = mockk<SseEmitterRegistry>(relaxed = true)
    private val notificationService = NotificationService(notificationRepository, emitterRegistry)

    @Test
    fun `구독하면 emitter를 레지스트리에 등록하고 반환한다`() {
        val emitter = notificationService.subscribe(1L)

        assertNotNull(emitter)
        verify(exactly = 1) { emitterRegistry.register(1L, any<SseEmitter>()) }
    }

    @Test
    fun `알림을 보내면 저장하고 구독 중인 emitter로도 전달한다`() {
        val savedSlot = slot<Notification>()
        every { notificationRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val notification = notificationService.notify(userId = 1L, orderId = 100L, message = "주문이 접수되었습니다.")

        assertEquals(1L, notification.userId)
        assertEquals(100L, notification.orderId)
        assertEquals("주문이 접수되었습니다.", notification.message)
        verify(exactly = 1) { emitterRegistry.send(1L, "notification", notification) }
    }

    @Test
    fun `내 알림 목록을 조회하면 그 사용자의 알림만 최신순으로 반환한다`() {
        val notification = Notification.withId(1L, userId = 1L, orderId = 100L, message = "주문이 접수되었습니다.")
        every { notificationRepository.findAllByUserIdOrderByCreatedAtDesc(1L) } returns listOf(notification)

        val actual = notificationService.getMyNotifications(1L)

        assertEquals(listOf(notification), actual)
    }
}
