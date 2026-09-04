package delivery.settlement.batch

import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.order.domain.Order
import delivery.order.domain.OrderItem
import delivery.order.domain.OrderStatus
import delivery.order.infrastructure.OrderItemRepository
import delivery.order.infrastructure.OrderRepository
import delivery.settlement.application.dayRange
import delivery.settlement.application.weekRange
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.SettlementRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 53-6 배치(Job) 전체가 실제로 리더/프로세서/컨텍스트까지 맞물려 도는지 검증한다 —
// 서비스 단위 테스트(ShopSettlementServiceTest 등)는 이미 계산 로직을 검증하지만,
// 여기서는 "그 기간에 활동이 있었던 대상만 찾아서" + "이미 정산된 대상은 건너뛰고"가
// Job 전체 관점에서 실제로 동작하는지가 관심사다.
@SpringBatchTest
class SettlementBatchJobIntegrationTest(
    @Autowired private val jobLauncherTestUtils: JobLauncherTestUtils,
    @Autowired @Qualifier("riderSettlementJob") private val riderSettlementJob: Job,
    @Autowired @Qualifier("shopSettlementJob") private val shopSettlementJob: Job,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val orderItemRepository: OrderItemRepository,
    @Autowired private val settlementRepository: SettlementRepository,
) : IntegrationTestSupport() {

    private val zone = ZoneId.of("Asia/Seoul")

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

    private fun deliveredOrder(shopId: Long, deliveredAt: Instant): Order {
        val order = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        order.transitionTo(OrderStatus.PAID)
        order.transitionTo(OrderStatus.ACCEPTED)
        order.transitionTo(OrderStatus.RIDER_ASSIGNED)
        order.transitionTo(OrderStatus.PICKED_UP)
        order.transitionTo(OrderStatus.DELIVERED)
        order.updatedAt = deliveredAt
        orderRepository.save(order)
        orderItemRepository.save(OrderItem(orderId = order.id!!, menuId = 1L, menuName = "짜장면", menuPrice = 8000L, quantity = 1))
        return order
    }

    @Test
    fun `라이더 정산 배치는 그날 배달완료한 라이더만 정산한다`() {
        val activeRider = System.nanoTime()
        val date = LocalDate.of(2026, 3, 15)
        val noon = date.atTime(12, 0).atZone(zone).toInstant()
        deliveredDelivery(activeRider, noon)

        jobLauncherTestUtils.job = riderSettlementJob
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder().addString("date", date.toString()).addLong("uniquifier", System.nanoTime()).toJobParameters()
        )

        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.status)
        val (start, end) = dayRange(date)
        val settlement = settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(
            SettlementTargetType.RIDER, activeRider, start, end,
        )
        assertEquals(activeRider, settlement?.targetId)
    }

    @Test
    fun `라이더 정산 배치는 이미 정산이 있는 라이더는 건너뛰고 나머지는 계속 처리한다`() {
        val alreadySettledRider = System.nanoTime()
        val newRider = System.nanoTime()
        val date = LocalDate.of(2026, 3, 16)
        val noon = date.atTime(12, 0).atZone(zone).toInstant()
        deliveredDelivery(alreadySettledRider, noon)
        deliveredDelivery(newRider, noon)
        val (start, end) = dayRange(date)
        settlementRepository.save(Settlement(SettlementTargetType.RIDER, alreadySettledRider, start, end, totalAmount = 0L))

        jobLauncherTestUtils.job = riderSettlementJob
        val execution = jobLauncherTestUtils.launchJob(
            JobParametersBuilder().addString("date", date.toString()).addLong("uniquifier", System.nanoTime()).toJobParameters()
        )

        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.status)
        val newRiderSettlement = settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(
            SettlementTargetType.RIDER, newRider, start, end,
        )
        assertEquals(newRider, newRiderSettlement?.targetId)
    }

    @Test
    fun `상점 정산 배치는 그 주에 배달완료된 주문이 없는 상점은 정산을 만들지 않는다`() {
        val inactiveShopId = System.nanoTime()
        val monday = LocalDate.of(2026, 4, 6)

        jobLauncherTestUtils.job = shopSettlementJob
        jobLauncherTestUtils.launchJob(
            JobParametersBuilder().addString("weekStart", monday.toString()).addLong("uniquifier", System.nanoTime()).toJobParameters()
        )

        val (start, end) = weekRange(monday)
        val settlement = settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(
            SettlementTargetType.SHOP, inactiveShopId, start, end,
        )
        assertNull(settlement)
    }
}
