package delivery.order.application

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// 1분마다 훑어서 일정 시간 넘게 PAID로 머문(사장님이 반응하지 않은) 주문을 자동 취소한다.
@Component
class StaleOrderCancellationScheduler(
    private val orderService: OrderService,
) {
    @Scheduled(fixedDelay = 60_000)
    fun cancelStaleOrders() {
        orderService.autoCancelStaleOrders()
    }
}
