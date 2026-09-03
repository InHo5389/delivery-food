package delivery.delivery.application

import delivery.delivery.application.dto.CreateDeliveryCommand
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.infrastructure.DeliveryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeliveryServiceTest {

    private val deliveryRepository = mockk<DeliveryRepository>()
    private lateinit var deliveryService: DeliveryService

    @BeforeEach
    fun setUp() {
        deliveryService = DeliveryService(deliveryRepository)
    }

    @Test
    fun `배달을 생성하면 PENDING 상태로 저장된다`() {
        val command = CreateDeliveryCommand(
            orderId = 1L, shopId = 1L,
            pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"),
            estimatedCookingMinutes = 15,
        )
        every { deliveryRepository.save(any()) } answers { it.invocation.args[0] as Delivery }

        val actual = deliveryService.createDelivery(command)

        assertEquals(DeliveryStatus.PENDING, actual.status)
        assertEquals(1L, actual.orderId)
        assertEquals(1L, actual.shopId)
        assertTrue(actual.estimatedPickupAt != null && actual.estimatedPickupAt!!.isAfter(java.time.Instant.now()))
    }
}
