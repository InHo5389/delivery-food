package delivery.delivery.infrastructure

import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeliveryAssignmentRepositoryIntegrationTest(
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val deliveryAssignmentRepository: DeliveryAssignmentRepository,
) : IntegrationTestSupport() {

    private fun offeringDelivery(): Delivery {
        val delivery = deliveryRepository.save(
            Delivery(orderId = System.nanoTime(), shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        return deliveryRepository.save(delivery)
    }

    @Test
    fun `OFFERING이고 라이더가 없으면 배정에 성공한다`() {
        val delivery = offeringDelivery()

        val actual = deliveryAssignmentRepository.tryAssignRider(delivery.id!!, 1L)

        assertTrue(actual)
        val persisted = deliveryRepository.findById(delivery.id!!).orElseThrow()
        assertEquals(DeliveryStatus.ASSIGNED, persisted.status)
        assertEquals(1L, persisted.riderId)
    }

    @Test
    fun `이미 라이더가 배정된 배달은 다시 배정할 수 없다`() {
        val delivery = offeringDelivery()
        deliveryAssignmentRepository.tryAssignRider(delivery.id!!, 1L)

        val actual = deliveryAssignmentRepository.tryAssignRider(delivery.id!!, 2L)

        assertFalse(actual)
        val persisted = deliveryRepository.findById(delivery.id!!).orElseThrow()
        assertEquals(1L, persisted.riderId)
    }

    @Test
    fun `PENDING 상태의 배달은 배정할 수 없다`() {
        val delivery = deliveryRepository.save(
            Delivery(orderId = System.nanoTime(), shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )

        val actual = deliveryAssignmentRepository.tryAssignRider(delivery.id!!, 1L)

        assertFalse(actual)
    }

    @Test
    fun `존재하지 않는 배달을 배정하려 하면 실패한다`() {
        val actual = deliveryAssignmentRepository.tryAssignRider(999_999L, 1L)

        assertFalse(actual)
    }
}
