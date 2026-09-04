package delivery.delivery.infrastructure.batch

import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.support.IntegrationTestSupport
import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Test
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.batch.infrastructure.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeliveredRiderIdReaderConfigIntegrationTest(
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val entityManagerFactory: EntityManagerFactory,
) : IntegrationTestSupport() {

    private val zone = ZoneId.of("Asia/Seoul")
    private val config = DeliveredRiderIdReaderConfig()

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

    private fun readAll(date: LocalDate): List<Long> {
        val reader = config.deliveredRiderIdReader(entityManagerFactory, date.toString()) as ItemStreamReader<Long>
        val items = mutableListOf<Long>()
        reader.open(ExecutionContext())
        try {
            while (true) {
                val item = reader.read() ?: break
                items.add(item)
            }
        } finally {
            reader.close()
        }
        return items
    }

    @Test
    fun `그날 완료된 배달의 라이더 id를 중복 없이 반환한다`() {
        val riderId = System.nanoTime()
        val date = LocalDate.of(2026, 3, 15)
        val noon = date.atTime(12, 0).atZone(zone).toInstant()
        // 같은 라이더가 그날 두 건 완료해도 한 번만 나와야 한다.
        deliveredDelivery(riderId, noon)
        deliveredDelivery(riderId, noon.plusSeconds(3600))
        // 범위 밖(전날)
        deliveredDelivery(riderId, noon.minusSeconds(86400))

        val actual = readAll(date)

        assertEquals(listOf(riderId), actual)
    }

    @Test
    fun `그날 완료된 배달이 없으면 빈 목록을 반환한다`() {
        val date = LocalDate.of(2026, 3, 16)

        val actual = readAll(date)

        assertTrue(actual.isEmpty())
    }
}
