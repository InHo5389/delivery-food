package delivery.delivery.application

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// 배차 매칭 엔진의 5초 주기 스케줄러.
// 실행 자체는 상태 없는 트리거일 뿐이라 로직은 DispatchService에 둔다 — 스케줄러를
// 테스트하려고 Spring 스케줄링 컨텍스트를 띄우지 않아도 DispatchService만 단위 테스트하면 된다.
@Component
class DispatchScheduler(
    private val dispatchService: DispatchService,
) {
    @Scheduled(fixedDelay = 5000)
    fun dispatch() {
        dispatchService.runDispatchCycle()
    }
}
