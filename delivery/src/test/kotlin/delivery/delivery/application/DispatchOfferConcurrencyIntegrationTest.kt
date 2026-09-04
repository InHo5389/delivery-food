package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.dto.AcceptOfferCommand
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.DispatchOffer
import delivery.delivery.domain.DispatchOfferStatus
import delivery.delivery.domain.Rider
import delivery.delivery.domain.RiderStatus
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.RiderRepository
import delivery.order.domain.Order
import delivery.order.domain.OrderStatus
import delivery.order.infrastructure.OrderRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

// 같은 배달 건에 라이더 여러 명이 동시에 수락을 누르면 정확히 1명만 성공해야 한다
// (가이드 문서의 동시성 제어 비교 소재). 스레드 100개를 동시에 띄우는 무거운 테스트라
// CLAUDE.md 규칙에 따라 slow로 표시한다.
@Tag("slow")
class DispatchOfferConcurrencyIntegrationTest(
    @Autowired private val dispatchOfferService: DispatchOfferService,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val riderRepository: RiderRepository,
    @Autowired private val dispatchOfferRepository: DispatchOfferRepository,
    @Autowired private val orderRepository: OrderRepository,
) : IntegrationTestSupport() {

    private fun acceptedOrder(): Order {
        val order = orderRepository.save(Order(System.nanoTime(), 1L, "홍길동", "01011112222"))
        order.transitionTo(OrderStatus.PAID)
        order.transitionTo(OrderStatus.ACCEPTED)
        return orderRepository.save(order)
    }

    @Test
    fun `동시 100명이 같은 배달을 수락해도 정확히 1명만 성공한다`() {
        val concurrency = 100
        val delivery = deliveryRepository.save(
            Delivery(orderId = acceptedOrder().id!!, shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        deliveryRepository.save(delivery)

        val offers = (1..concurrency).map { i ->
            val rider = riderRepository.save(
                Rider(System.nanoTime(), BigDecimal("37.5665000"), BigDecimal("126.9780000"), status = RiderStatus.AVAILABLE)
            )
            dispatchOfferRepository.save(DispatchOffer(deliveryId = delivery.id!!, riderId = rider.id!!))
        }

        val successCount = AtomicInteger(0)
        val conflictCount = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(concurrency)
        val readyLatch = CountDownLatch(concurrency)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(concurrency)

        offers.forEach { offer ->
            executor.submit {
                readyLatch.countDown()
                startLatch.await()
                try {
                    dispatchOfferService.accept(AcceptOfferCommand(offer.id!!, offer.riderId))
                    successCount.incrementAndGet()
                } catch (e: BusinessException) {
                    conflictCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        readyLatch.await(10, TimeUnit.SECONDS)
        startLatch.countDown()
        doneLatch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(1, successCount.get())
        assertEquals(concurrency - 1, conflictCount.get())
        val persisted = deliveryRepository.findById(delivery.id!!).orElseThrow()
        assertEquals(DeliveryStatus.ASSIGNED, persisted.status)
    }
}
