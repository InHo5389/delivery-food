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

class DeliveryRepositoryIntegrationTest(
    @Autowired private val deliveryRepository: DeliveryRepository,
) : IntegrationTestSupport() {

    private fun deliveredDelivery(riderId: Long, deliveredAt: Instant, orderId: Long = System.nanoTime()): Delivery {
        val delivery = deliveryRepository.save(
            Delivery(orderId = orderId, shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        delivery.transitionTo(DeliveryStatus.ASSIGNED)
        delivery.riderId = riderId
        delivery.transitionTo(DeliveryStatus.PICKED_UP)
        delivery.transitionTo(DeliveryStatus.DELIVERED)
        delivery.updatedAt = deliveredAt
        return deliveryRepository.save(delivery)
    }

    @Test
    fun `범위 안에서 라이더가 완료한 배달의 주문 ID를 반환한다`() {
        val riderId = System.nanoTime()
        val now = Instant.now()
        val delivery = deliveredDelivery(riderId, now, orderId = 101L)
        // 범위 밖(어제)
        deliveredDelivery(riderId, now.minus(1, ChronoUnit.DAYS), orderId = 102L)

        val actual = deliveryRepository.findAllByRiderIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            riderId, DeliveryStatus.DELIVERED, now.minusSeconds(3600), now.plusSeconds(3600),
        )

        assertEquals(1, actual.size)
        assertEquals(delivery.orderId, actual[0].orderId)
    }

    @Test
    fun `다른 라이더의 배달은 제외된다`() {
        val riderId = System.nanoTime()
        val now = Instant.now()
        deliveredDelivery(riderId + 1, now, orderId = 201L)

        val actual = deliveryRepository.findAllByRiderIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            riderId, DeliveryStatus.DELIVERED, now.minusSeconds(60), now.plusSeconds(60),
        )

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `DELIVERED가 아닌 배달은 제외된다`() {
        val riderId = System.nanoTime()
        val now = Instant.now()
        val delivery = deliveryRepository.save(
            Delivery(orderId = 301L, shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        delivery.transitionTo(DeliveryStatus.ASSIGNED)
        delivery.riderId = riderId
        delivery.updatedAt = now
        deliveryRepository.save(delivery)

        val actual = deliveryRepository.findAllByRiderIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            riderId, DeliveryStatus.DELIVERED, now.minusSeconds(60), now.plusSeconds(60),
        )

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `완료한 배달이 없는 라이더를 조회하면 빈 목록을 반환한다`() {
        val actual = deliveryRepository.findAllByRiderIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            Long.MAX_VALUE, DeliveryStatus.DELIVERED, Instant.EPOCH, Instant.now(),
        )

        assertTrue(actual.isEmpty())
    }
}
