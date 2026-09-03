package delivery.delivery.infrastructure

import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatchQueueRepositoryIntegrationTest(
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val dispatchQueueRepository: DispatchQueueRepository,
) : IntegrationTestSupport() {

    private fun saveDelivery(status: DeliveryStatus, createdAt: Instant): Delivery {
        val delivery = deliveryRepository.save(
            Delivery(
                orderId = System.nanoTime(),
                shopId = 1L,
                pickupLatitude = BigDecimal("37.5665000"),
                pickupLongitude = BigDecimal("126.9780000"),
                createdAt = createdAt,
            )
        )
        if (status != DeliveryStatus.PENDING) {
            delivery.transitionTo(status)
            deliveryRepository.save(delivery)
        }
        return delivery
    }

    @Test
    fun `OFFERING 상태의 배달만 큐에 포함된다`() {
        val offering = saveDelivery(DeliveryStatus.OFFERING, Instant.now())
        val pending = saveDelivery(DeliveryStatus.PENDING, Instant.now())

        val actual = dispatchQueueRepository.findQueue(limit = 100)

        assertTrue(actual.any { it.deliveryId == offering.id })
        assertTrue(actual.none { it.deliveryId == pending.id })
    }

    @Test
    fun `먼저 배차 요청된 배달이 큐 앞쪽에 온다`() {
        val now = Instant.now()
        val older = saveDelivery(DeliveryStatus.OFFERING, now.minus(10, ChronoUnit.MINUTES))
        val newer = saveDelivery(DeliveryStatus.OFFERING, now)

        val actual = dispatchQueueRepository.findQueue(limit = 100)

        val olderIndex = actual.indexOfFirst { it.deliveryId == older.id }
        val newerIndex = actual.indexOfFirst { it.deliveryId == newer.id }
        assertTrue(olderIndex < newerIndex)
    }

    @Test
    fun `limit만큼만 조회된다`() {
        repeat(5) { saveDelivery(DeliveryStatus.OFFERING, Instant.now()) }

        val actual = dispatchQueueRepository.findQueue(limit = 2)

        assertEquals(2, actual.size)
    }

    @Test
    fun `claimNext는 가장 오래된 OFFERING 배달을 반환한다`() {
        // 다른 테스트가 이 컨테이너에 이미 심어둔 OFFERING 배달보다도 확실히 오래되도록
        // EPOCH를 기준으로 잡는다 — 같은 MySQL 컨테이너를 여러 통합 테스트가 공유하기 때문.
        val older = saveDelivery(DeliveryStatus.OFFERING, Instant.EPOCH)
        saveDelivery(DeliveryStatus.OFFERING, Instant.now())

        val actual = dispatchQueueRepository.claimNext()

        assertEquals(older.id, actual?.deliveryId)

        // claimNext는 잠그기만 하고 상태를 바꾸지 않는다. 여기서 직접 소비 처리해두지
        // 않으면 이 EPOCH짜리 배달이 계속 "가장 오래된 후보"로 남아 다른 테스트의
        // claimNext 결과를 오염시킨다.
        older.transitionTo(DeliveryStatus.ASSIGNED)
        deliveryRepository.save(older)
    }

    @Test
    fun `claimNext는 ASSIGNED 상태의 배달은 후보로 보지 않는다`() {
        val assigned = saveDelivery(DeliveryStatus.ASSIGNED, Instant.now().minus(1, ChronoUnit.HOURS))

        val actual = dispatchQueueRepository.claimNext()

        assertTrue(actual == null || actual.deliveryId != assigned.id)
    }
}
