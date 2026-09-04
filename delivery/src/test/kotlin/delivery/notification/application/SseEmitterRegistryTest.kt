package delivery.notification.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

class SseEmitterRegistryTest {

    private val registry = SseEmitterRegistry()

    @Test
    fun `등록한 emitter로 이벤트를 보낸다`() {
        val emitter = mockk<SseEmitter>(relaxed = true)
        registry.register(1L, emitter)

        registry.send(1L, "notification", "hello")

        verify(exactly = 1) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `같은 사용자가 여러 emitter를 등록하면 전부에게 보낸다`() {
        val emitter1 = mockk<SseEmitter>(relaxed = true)
        val emitter2 = mockk<SseEmitter>(relaxed = true)
        registry.register(1L, emitter1)
        registry.register(1L, emitter2)

        registry.send(1L, "notification", "hello")

        verify(exactly = 1) { emitter1.send(any<SseEmitter.SseEventBuilder>()) }
        verify(exactly = 1) { emitter2.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `구독하지 않은 사용자에게 보내도 예외가 발생하지 않는다`() {
        registry.send(999L, "notification", "hello")
    }

    @Test
    fun `다른 사용자에게 등록된 emitter로는 보내지 않는다`() {
        val emitter = mockk<SseEmitter>(relaxed = true)
        registry.register(1L, emitter)

        registry.send(2L, "notification", "hello")

        verify(exactly = 0) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `전송 중 IOException이 발생하면 해당 emitter를 레지스트리에서 제거한다`() {
        val emitter = mockk<SseEmitter>(relaxed = true)
        every { emitter.send(any<SseEmitter.SseEventBuilder>()) } throws IOException("broken pipe")
        registry.register(1L, emitter)

        registry.send(1L, "notification", "hello")
        registry.send(1L, "notification", "hello again")

        verify(exactly = 1) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }
}
