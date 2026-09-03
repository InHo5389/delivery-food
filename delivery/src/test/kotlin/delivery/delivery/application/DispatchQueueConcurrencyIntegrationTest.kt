package delivery.delivery.application

import delivery.common.exception.BusinessException
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.Rider
import delivery.delivery.domain.RiderStatus
import delivery.delivery.infrastructure.DeliveryAssignmentRepository
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.DispatchQueueRepository
import delivery.delivery.infrastructure.RiderRepository
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

// 큐에 배달이 1건뿐인데 라이더 여러 명이 동시에 클레임하면 정확히 1명만 성공해야 한다
// (FOR UPDATE가 만드는 직렬화를 증명하는 소재). 스레드를 여러 개 띄우는 무거운 테스트라
// CLAUDE.md 규칙에 따라 slow로 표시한다.
@Tag("slow")
class DispatchQueueConcurrencyIntegrationTest(
    @Autowired private val dispatchQueueService: DispatchQueueService,
    @Autowired private val dispatchQueueRepository: DispatchQueueRepository,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val deliveryAssignmentRepository: DeliveryAssignmentRepository,
    @Autowired private val riderRepository: RiderRepository,
) : IntegrationTestSupport() {

    // 같은 MySQL 컨테이너를 다른 통합 테스트들과 공유하기 때문에, 그 테스트들이 남겨둔
    // OFFERING 배달이 남아 있으면 "큐에 1건뿐"이라는 이 테스트의 전제가 깨진다.
    // 시작 전에 큐를 비운다(더미 라이더로 전부 소비).
    private fun drainQueue() {
        val vacuumRider = riderRepository.save(
            Rider(System.nanoTime(), BigDecimal("37.5665000"), BigDecimal("126.9780000"), status = RiderStatus.AVAILABLE)
        )
        while (true) {
            val next = dispatchQueueRepository.claimNext() ?: break
            deliveryAssignmentRepository.tryAssignRider(next.deliveryId, vacuumRider.id!!)
        }
    }

    @Test
    fun `큐에 배달이 1건뿐이어도 동시에 클레임하는 라이더는 정확히 1명만 성공한다`() {
        drainQueue()
        val concurrency = 20
        val delivery = deliveryRepository.save(
            Delivery(orderId = System.nanoTime(), shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        deliveryRepository.save(delivery)

        val riderAccountIds = (1..concurrency).map { i ->
            val accountId = System.nanoTime() + i
            riderRepository.save(Rider(accountId, BigDecimal("37.5665000"), BigDecimal("126.9780000"), status = RiderStatus.AVAILABLE))
            accountId
        }

        val successCount = AtomicInteger(0)
        val emptyQueueCount = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(concurrency)
        val readyLatch = CountDownLatch(concurrency)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(concurrency)

        riderAccountIds.forEach { accountId ->
            executor.submit {
                readyLatch.countDown()
                startLatch.await()
                try {
                    dispatchQueueService.claim(accountId)
                    successCount.incrementAndGet()
                } catch (e: BusinessException) {
                    emptyQueueCount.incrementAndGet()
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
        assertEquals(concurrency - 1, emptyQueueCount.get())
        val persisted = deliveryRepository.findById(delivery.id!!).orElseThrow()
        assertEquals(DeliveryStatus.ASSIGNED, persisted.status)
    }
}
