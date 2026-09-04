package delivery.settlement.application

import delivery.common.exception.BusinessException
import delivery.delivery.application.DeliveryService
import delivery.order.application.OrderService
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementErrorCode
import delivery.settlement.domain.SettlementItem
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.SettlementItemRepository
import delivery.settlement.infrastructure.SettlementRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RiderSettlementServiceTest {

    private val settlementRepository = mockk<SettlementRepository>()
    private val settlementItemRepository = mockk<SettlementItemRepository>()
    private val deliveryService = mockk<DeliveryService>()
    private val orderService = mockk<OrderService>()
    private lateinit var riderSettlementService: RiderSettlementService

    private val riderId = 1L
    private val march = YearMonth.of(2026, 3)

    @BeforeEach
    fun setUp() {
        riderSettlementService = RiderSettlementService(settlementRepository, settlementItemRepository, deliveryService, orderService)

        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(any(), any(), any(), any()) } returns null
        every { settlementItemRepository.saveAll(any<List<SettlementItem>>()) } returns emptyList()
        every { settlementRepository.save(any()) } answers { (it.invocation.args[0] as Settlement).also { s -> setId(s, 100L) } }
    }

    private fun setId(settlement: Settlement, id: Long): Settlement {
        val field = Settlement::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(settlement, id)
        return settlement
    }

    @Test
    fun `완료한 배달의 배달비를 모두 합산해 정산액이 된다`() {
        every { deliveryService.getDeliveredOrderIds(riderId, any(), any()) } returns listOf(101L, 102L)
        every { orderService.getDeliveryFees(listOf(101L, 102L)) } returns mapOf(101L to 3000L, 102L to 2500L)

        val actual = riderSettlementService.calculateRiderSettlement(riderId, march)

        assertEquals(5500L, actual.totalAmount)
        assertEquals(SettlementTargetType.RIDER, actual.targetType)
    }

    @Test
    fun `완료한 배달이 없는 달은 0원이다`() {
        every { deliveryService.getDeliveredOrderIds(riderId, any(), any()) } returns emptyList()
        every { orderService.getDeliveryFees(emptyList()) } returns emptyMap()

        val actual = riderSettlementService.calculateRiderSettlement(riderId, march)

        assertEquals(0L, actual.totalAmount)
    }

    @Test
    fun `배달비가 0원인 배달은 정산액에 0으로 반영된다`() {
        every { deliveryService.getDeliveredOrderIds(riderId, any(), any()) } returns listOf(101L)
        every { orderService.getDeliveryFees(listOf(101L)) } returns mapOf(101L to 0L)

        val actual = riderSettlementService.calculateRiderSettlement(riderId, march)

        assertEquals(0L, actual.totalAmount)
    }

    @Test
    fun `요율 없이 배달비 전액이 그대로 정산 항목 금액이 된다`() {
        every { deliveryService.getDeliveredOrderIds(riderId, any(), any()) } returns listOf(101L)
        every { orderService.getDeliveryFees(listOf(101L)) } returns mapOf(101L to 3000L)
        val itemsSlot = slot<List<SettlementItem>>()
        every { settlementItemRepository.saveAll(capture(itemsSlot)) } returns emptyList()

        riderSettlementService.calculateRiderSettlement(riderId, march)

        val item = itemsSlot.captured.single()
        assertEquals(BigDecimal("0.0000"), item.appliedFeeRate)
        assertEquals(3000L, item.amount)
        assertEquals(3000L, item.settlementAmount)
    }

    @Test
    fun `이미 같은 기간의 정산이 있으면 예외가 발생한다`() {
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.RIDER, riderId, any(), any()) } returns
            Settlement.withId(1L, SettlementTargetType.RIDER, riderId, Instant.now(), Instant.now().plusSeconds(1))

        val exception = assertThrows<BusinessException> { riderSettlementService.calculateRiderSettlement(riderId, march) }

        assertEquals(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `월 경계는 KST 기준 그 달 1일 00시부터 다음 달 1일 00시 직전까지다`() {
        val startSlot = slot<Instant>()
        val endSlot = slot<Instant>()
        every { deliveryService.getDeliveredOrderIds(riderId, capture(startSlot), capture(endSlot)) } returns emptyList()
        every { orderService.getDeliveryFees(any()) } returns emptyMap()

        riderSettlementService.calculateRiderSettlement(riderId, march)

        val zone = java.time.ZoneId.of("Asia/Seoul")
        assertTrue(startSlot.captured == march.atDay(1).atStartOfDay(zone).toInstant())
        assertTrue(endSlot.captured == march.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant())
    }
}
