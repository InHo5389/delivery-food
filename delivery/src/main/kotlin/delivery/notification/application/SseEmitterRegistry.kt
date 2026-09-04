package delivery.notification.application

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// ⚠️ 의도적 구식 구현(04_Phase1_기본틀.md 5부 참조) — 동기 서블릿 기반 SSE다. 연결
// 하나당 요청 스레드 하나를 계속 점유한다(Phase 3 6-4절에서 비동기 서블릿으로 개선 예정 —
// 그때 가서 "SSE 외 API의 p95"가 개선의 증거 지표가 된다).
//
// 같은 사용자가 탭을 여러 개 열 수 있어 userId 하나에 emitter 여러 개를 허용한다.
// 서버가 여러 대로 늘어나면(수평 확장) 이 in-memory 레지스트리로는 다른 서버에 붙은
// 연결에 못 보낸다 — MVP는 단일 인스턴스를 전제로 하고, 이 한계는 이 인스턴스가
// 자신에게 붙은 연결만 책임지는 것으로 받아들인다(Phase 5 이전에는 다중 인스턴스 운영 없음).
@Component
class SseEmitterRegistry {
    private val emittersByUserId = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    fun register(userId: Long, emitter: SseEmitter) {
        val emitters = emittersByUserId.computeIfAbsent(userId) { CopyOnWriteArrayList() }
        emitters.add(emitter)
        emitter.onCompletion { remove(userId, emitter) }
        emitter.onTimeout { remove(userId, emitter) }
        emitter.onError { remove(userId, emitter) }
    }

    fun send(userId: Long, eventName: String, data: Any) {
        val emitters = emittersByUserId[userId] ?: return
        emitters.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data))
            } catch (e: IOException) {
                // 클라이언트가 이미 연결을 끊은 뒤 보내려 한 것 — 정상적인 상황이라 경고 없이 정리만 한다.
                remove(userId, emitter)
            }
        }
    }

    private fun remove(userId: Long, emitter: SseEmitter) {
        emittersByUserId[userId]?.remove(emitter)
    }
}
