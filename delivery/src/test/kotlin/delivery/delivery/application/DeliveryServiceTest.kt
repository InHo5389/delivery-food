package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.dto.CreateDeliveryCommand
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryErrorCode
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.Rider
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.RiderRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeliveryServiceTest {

    private val deliveryRepository = mockk<DeliveryRepository>()
    private val riderRepository = mockk<RiderRepository>()
    private lateinit var deliveryService: DeliveryService

    @BeforeEach
    fun setUp() {
        deliveryService = DeliveryService(deliveryRepository, riderRepository)
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

    @Test
    fun `accountId로 riderId를 조회한다`() {
        every { riderRepository.findByAccountId(100L) } returns Rider.withId(1L, 100L)

        val actual = deliveryService.getRiderIdByAccountId(100L)

        assertEquals(1L, actual)
    }

    @Test
    fun `라이더 프로필이 없는 accountId로 조회하면 예외가 발생한다`() {
        every { riderRepository.findByAccountId(999L) } returns null

        val exception = assertThrows<BusinessException> { deliveryService.getRiderIdByAccountId(999L) }

        assertEquals(DeliveryErrorCode.RIDER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `riderId로 accountId를 조회한다`() {
        every { riderRepository.findById(1L) } returns Optional.of(Rider.withId(1L, 100L))

        val actual = deliveryService.getRiderAccountId(1L)

        assertEquals(100L, actual)
    }

    @Test
    fun `존재하지 않는 riderId로 조회하면 null을 반환한다`() {
        every { riderRepository.findById(999L) } returns Optional.empty()

        val actual = deliveryService.getRiderAccountId(999L)

        assertNull(actual)
    }
}
