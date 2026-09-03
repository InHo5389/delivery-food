package delivery.delivery.application

import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.Rider
import delivery.delivery.domain.RiderStatus
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.RiderRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatchServiceIntegrationTest(
    @Autowired private val dispatchService: DispatchService,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val riderRepository: RiderRepository,
    @Autowired private val dispatchOfferRepository: DispatchOfferRepository,
) : IntegrationTestSupport() {

    private val pickupLat = BigDecimal("37.5665000")
    private val pickupLng = BigDecimal("126.9780000")

    private fun newDelivery(): Delivery = deliveryRepository.save(Delivery(orderId = System.nanoTime(), shopId = 1L, pickupLatitude = pickupLat, pickupLongitude = pickupLng))

    private fun availableRider(latitude: BigDecimal = BigDecimal("37.5666000"), longitude: BigDecimal = BigDecimal("126.9781000")): Rider =
        riderRepository.save(Rider(System.nanoTime(), latitude, longitude, status = RiderStatus.AVAILABLE))

    @Test
    fun `후보 라이더가 있으면 오퍼를 만들고 배달을 OFFERING으로 전이한다`() {
        val delivery = newDelivery()
        availableRider()

        val actual = dispatchService.dispatchOne(delivery.id!!)

        assertEquals(1, actual.offeredRiderIds.size)
        val persisted = deliveryRepository.findById(delivery.id!!).orElseThrow()
        assertEquals(DeliveryStatus.OFFERING, persisted.status)
        assertEquals(1, dispatchOfferRepository.findAllByDeliveryId(delivery.id!!).size)
    }

    @Test
    fun `후보가 없으면 오퍼 없이 PENDING을 유지한다`() {
        val delivery = newDelivery()

        val actual = dispatchService.dispatchOne(delivery.id!!)

        assertTrue(actual.offeredRiderIds.isEmpty())
        val persisted = deliveryRepository.findById(delivery.id!!).orElseThrow()
        assertEquals(DeliveryStatus.PENDING, persisted.status)
        assertTrue(dispatchOfferRepository.findAllByDeliveryId(delivery.id!!).isEmpty())
    }

    @Test
    fun `반경 밖 라이더는 후보에서 제외되어 오퍼가 생성되지 않는다`() {
        val delivery = newDelivery()
        // 부산 좌표 — 서울시청 픽업 지점(반경 3km) 밖
        availableRider(BigDecimal("35.1796000"), BigDecimal("129.0756000"))

        val actual = dispatchService.dispatchOne(delivery.id!!)

        assertTrue(actual.offeredRiderIds.isEmpty())
    }

    @Test
    fun `상위 3명에게만 오퍼를 보낸다`() {
        val delivery = newDelivery()
        repeat(5) { i -> availableRider(BigDecimal("37.566${i}000"), BigDecimal("126.978${i}000")) }

        val actual = dispatchService.dispatchOne(delivery.id!!)

        assertEquals(3, actual.offeredRiderIds.size)
        assertEquals(3, dispatchOfferRepository.findAllByDeliveryId(delivery.id!!).size)
    }

    @Test
    fun `이미 오퍼받은 라이더는 재배차 시 제외된다`() {
        val delivery = newDelivery()
        availableRider()

        val first = dispatchService.dispatchOne(delivery.id!!)
        val second = dispatchService.dispatchOne(delivery.id!!)

        assertEquals(1, first.offeredRiderIds.size)
        assertTrue(second.offeredRiderIds.isEmpty())
        assertEquals(1, dispatchOfferRepository.findAllByDeliveryId(delivery.id!!).size)
    }

    @Test
    fun `runDispatchCycle은 PENDING 배달을 모두 처리한다`() {
        val delivery1 = newDelivery()
        val delivery2 = newDelivery()
        availableRider()

        val actual = dispatchService.runDispatchCycle()

        assertEquals(setOf(delivery1.id, delivery2.id), actual.map { it.deliveryId }.toSet())
    }
}
