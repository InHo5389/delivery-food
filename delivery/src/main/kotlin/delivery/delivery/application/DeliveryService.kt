package delivery.delivery.application

import delivery.delivery.application.dto.CreateDeliveryCommand
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.infrastructure.DeliveryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
) {
    // order 모듈이 주문 접수(ACCEPTED) 시점에 호출한다. 배차 매칭 스케줄러가 5초마다
    // PENDING 배달을 훑으므로, 이 호출 이후 다음 사이클 안에 오퍼가 나가기 시작한다.
    // estimatedPickupAt은 사장님이 입력한 조리 예상 시간을 "지금부터 N분 후"로 환산한
    // 값이다 — 라이더가 배차 큐에서 이 시각을 보고 언제쯤 출발하면 될지 가늠한다.
    @Transactional
    fun createDelivery(command: CreateDeliveryCommand): Delivery =
        deliveryRepository.save(
            Delivery(
                orderId = command.orderId,
                shopId = command.shopId,
                pickupLatitude = command.pickupLatitude,
                pickupLongitude = command.pickupLongitude,
                estimatedPickupAt = Instant.now().plusSeconds(command.estimatedCookingMinutes * 60L),
            )
        )

    // settlement 모듈이 라이더 정산을 계산할 때 쓴다. 배달비 금액 자체는 order 모듈이
    // 갖고 있어(Order.deliveryFee) 여기서는 "이 라이더가 이 기간에 완료한 배달이 어느
    // 주문들인지"만 돌려주고, 금액 조회는 settlement가 order 모듈에 별도로 요청한다.
    fun getDeliveredOrderIds(riderId: Long, from: Instant, to: Instant): List<Long> =
        deliveryRepository
            .findAllByRiderIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(riderId, DeliveryStatus.DELIVERED, from, to)
            .map { it.orderId }
}
