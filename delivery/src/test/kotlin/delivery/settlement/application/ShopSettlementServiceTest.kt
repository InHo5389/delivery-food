package delivery.settlement.application

import delivery.common.exception.BusinessException
import delivery.order.application.OrderService
import delivery.order.application.dto.ShopSettlementSourceItem
import delivery.settlement.domain.CommissionRate
import delivery.settlement.domain.RateType
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementErrorCode
import delivery.settlement.domain.SettlementItem
import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.CommissionRateRepository
import delivery.settlement.infrastructure.SettlementItemRepository
import delivery.settlement.infrastructure.SettlementRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopSettlementServiceTest {

    private val settlementRepository = mockk<SettlementRepository>()
    private val settlementItemRepository = mockk<SettlementItemRepository>()
    private val commissionRateRepository = mockk<CommissionRateRepository>()
    private val orderService = mockk<OrderService>()
    private lateinit var shopSettlementService: ShopSettlementService

    private val shopId = 1L

    // 2026-03-09는 월요일이다(주 단위 정산 기간은 반드시 월요일부터 시작해야 한다).
    private val monday = LocalDate.of(2026, 3, 9)

    @BeforeEach
    fun setUp() {
        shopSettlementService = ShopSettlementService(settlementRepository, settlementItemRepository, commissionRateRepository, orderService)

        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(any(), any(), any(), any()) } returns null
        every { settlementItemRepository.findTopByOrderIdAndTypeOrderByCreatedAtDesc(any(), any()) } returns null
        every { settlementItemRepository.saveAll(any<List<SettlementItem>>()) } returns emptyList()
        every { commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(any(), any()) } returns
            CommissionRate.withId(1L, RateType.PLATFORM_FEE, BigDecimal("0.2000"), Instant.parse("2026-01-01T00:00:00Z"))
        every { settlementRepository.save(any()) } answers { (it.invocation.args[0] as Settlement).also { s -> setId(s, 100L) } }
    }

    private fun setId(settlement: Settlement, id: Long): Settlement {
        val field = Settlement::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(settlement, id)
        return settlement
    }

    @Test
    fun `판매만 있고 환불 없으면 요율을 뗀 만큼 정산액이 된다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 10_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        val actual = shopSettlementService.calculateShopSettlement(shopId, monday)

        assertEquals(8_000L, actual.totalAmount)
        assertEquals(0L, actual.carriedOverAmount)
    }

    @Test
    fun `주의 시작이 월요일이 아니면 예외가 발생한다`() {
        val tuesday = monday.plusDays(1)

        assertThrows<IllegalArgumentException> { shopSettlementService.calculateShopSettlement(shopId, tuesday) }
    }

    @Test
    fun `환불액이 판매액과 같으면 정산액이 0원이 된다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 10_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 102L, amount = 10_000L))

        val actual = shopSettlementService.calculateShopSettlement(shopId, monday)

        assertEquals(0L, actual.totalAmount)
    }

    @Test
    fun `부분 환불이면 판매분과 환불분이 각각 요율만큼 차감돼 합산된다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 20_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 102L, amount = 10_000L))

        val actual = shopSettlementService.calculateShopSettlement(shopId, monday)

        // 판매 20,000 - 4,000(20%) = 16,000 / 환불 10,000 - 2,000(20%) = 8,000 차감 → 8,000
        assertEquals(8_000L, actual.totalAmount)
    }

    @Test
    fun `판매도 환불도 없는 주는 0원이다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns emptyList()
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        val actual = shopSettlementService.calculateShopSettlement(shopId, monday)

        assertEquals(0L, actual.totalAmount)
        assertEquals(SettlementTargetType.SHOP, actual.targetType)
    }

    @Test
    fun `환불이 판매보다 크면 정산액이 음수가 된다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 5_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 102L, amount = 20_000L))

        val actual = shopSettlementService.calculateShopSettlement(shopId, monday)

        assertTrue(actual.totalAmount < 0)
    }

    @Test
    fun `직전 주 정산액이 음수였으면 이번 주로 이월된다`() {
        val previousMonday = monday.minusWeeks(1)
        val (prevStart, prevEnd) = weekRangeFor(previousMonday)
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.SHOP, shopId, prevStart, prevEnd) } returns
            Settlement.withId(1L, SettlementTargetType.SHOP, shopId, prevStart, prevEnd, totalAmount = -3_000L)
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 10_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        val actual = shopSettlementService.calculateShopSettlement(shopId, monday)

        // 이번 주 판매 정산분 8,000 + 직전 주 이월 -3,000 = 5,000
        assertEquals(-3_000L, actual.carriedOverAmount)
        assertEquals(5_000L, actual.totalAmount)
    }

    @Test
    fun `직전 주 정산액이 양수였으면 이월되지 않는다`() {
        val previousMonday = monday.minusWeeks(1)
        val (prevStart, prevEnd) = weekRangeFor(previousMonday)
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.SHOP, shopId, prevStart, prevEnd) } returns
            Settlement.withId(1L, SettlementTargetType.SHOP, shopId, prevStart, prevEnd, totalAmount = 3_000L)
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns emptyList()
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        val actual = shopSettlementService.calculateShopSettlement(shopId, monday)

        assertEquals(0L, actual.carriedOverAmount)
    }

    @Test
    fun `동시에 같은 기간이 계산돼 저장 시점에 유니크 제약을 위반하면 예외가 발생한다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 10_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()
        every { settlementRepository.save(any()) } throws DataIntegrityViolationException("uk_settlement_target_period")

        val exception = assertThrows<BusinessException> { shopSettlementService.calculateShopSettlement(shopId, monday) }

        assertEquals(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `적용할 요율이 없으면 예외가 발생한다`() {
        every { commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(any(), any()) } returns null

        val exception = assertThrows<BusinessException> { shopSettlementService.calculateShopSettlement(shopId, monday) }

        assertEquals(SettlementErrorCode.COMMISSION_RATE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `주 단위 경계는 KST 기준 월요일 00시부터 다음 월요일 00시 직전까지다`() {
        val startSlot = slot<Instant>()
        val endSlot = slot<Instant>()
        every { orderService.getDeliveredOrderAmounts(shopId, capture(startSlot), capture(endSlot)) } returns emptyList()
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        shopSettlementService.calculateShopSettlement(shopId, monday)

        val (expectedStart, expectedEnd) = weekRangeFor(monday)
        assertEquals(expectedStart, startSlot.captured)
        assertEquals(expectedEnd, endSlot.captured)
    }

    @Test
    fun `환불 항목은 같은 주문의 과거 판매 요율이 있으면 그 요율을 재사용한다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns emptyList()
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 10_000L))
        every { settlementItemRepository.findTopByOrderIdAndTypeOrderByCreatedAtDesc(101L, SettlementItemType.SALE) } returns
            SettlementItem.withId(1L, 999L, 101L, SettlementItemType.SALE, amount = 10_000L, appliedFeeRate = BigDecimal("0.1000"), settlementAmount = 9_000L)

        val itemsSlot = slot<List<SettlementItem>>()
        every { settlementItemRepository.saveAll(capture(itemsSlot)) } returns emptyList()

        val actual = shopSettlementService.calculateShopSettlement(shopId, monday)

        // 이번 주 요율은 20%지만, 과거 판매 때 적용됐던 10%를 그대로 써서 -9,000이 된다.
        assertEquals(-9_000L, actual.totalAmount)
        assertEquals(BigDecimal("0.1000"), itemsSlot.captured.single().appliedFeeRate)
    }

    private fun weekRangeFor(weekStart: LocalDate): Pair<Instant, Instant> {
        val zone = ZoneId.of("Asia/Seoul")
        val start = weekStart.atStartOfDay(zone).toInstant()
        val end = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant()
        return start to end
    }
}
