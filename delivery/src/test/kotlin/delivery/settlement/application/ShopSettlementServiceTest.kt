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
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopSettlementServiceTest {

    private val settlementRepository = mockk<SettlementRepository>()
    private val settlementItemRepository = mockk<SettlementItemRepository>()
    private val commissionRateRepository = mockk<CommissionRateRepository>()
    private val orderService = mockk<OrderService>()
    private lateinit var shopSettlementService: ShopSettlementService

    private val shopId = 1L
    private val march = YearMonth.of(2026, 3)

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

        val actual = shopSettlementService.calculateShopSettlement(shopId, march)

        assertEquals(8_000L, actual.totalAmount)
        assertEquals(0L, actual.carriedOverAmount)
    }

    @Test
    fun `환불액이 판매액과 같으면 정산액이 0원이 된다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 10_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 102L, amount = 10_000L))

        val actual = shopSettlementService.calculateShopSettlement(shopId, march)

        assertEquals(0L, actual.totalAmount)
    }

    @Test
    fun `부분 환불이면 판매분과 환불분이 각각 요율만큼 차감돼 합산된다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 20_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 102L, amount = 10_000L))

        val actual = shopSettlementService.calculateShopSettlement(shopId, march)

        // 판매 20,000 - 4,000(20%) = 16,000 / 환불 10,000 - 2,000(20%) = 8,000 차감 → 8,000
        assertEquals(8_000L, actual.totalAmount)
    }

    @Test
    fun `판매도 환불도 없는 달은 0원이다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns emptyList()
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        val actual = shopSettlementService.calculateShopSettlement(shopId, march)

        assertEquals(0L, actual.totalAmount)
        assertEquals(SettlementTargetType.SHOP, actual.targetType)
    }

    @Test
    fun `환불이 판매보다 크면 정산액이 음수가 된다`() {
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 5_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 102L, amount = 20_000L))

        val actual = shopSettlementService.calculateShopSettlement(shopId, march)

        assertTrue(actual.totalAmount < 0)
    }

    @Test
    fun `직전 달 정산액이 음수였으면 이번 달로 이월된다`() {
        val february = march.minusMonths(1)
        val (febStart, febEnd) = monthRangeFor(february)
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.SHOP, shopId, febStart, febEnd) } returns
            Settlement.withId(1L, SettlementTargetType.SHOP, shopId, febStart, febEnd, totalAmount = -3_000L)
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns listOf(ShopSettlementSourceItem(orderId = 101L, amount = 10_000L))
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        val actual = shopSettlementService.calculateShopSettlement(shopId, march)

        // 이번 달 판매 정산분 8,000 + 직전 달 이월 -3,000 = 5,000
        assertEquals(-3_000L, actual.carriedOverAmount)
        assertEquals(5_000L, actual.totalAmount)
    }

    @Test
    fun `직전 달 정산액이 양수였으면 이월되지 않는다`() {
        val february = march.minusMonths(1)
        val (febStart, febEnd) = monthRangeFor(february)
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.SHOP, shopId, febStart, febEnd) } returns
            Settlement.withId(1L, SettlementTargetType.SHOP, shopId, febStart, febEnd, totalAmount = 3_000L)
        every { orderService.getDeliveredOrderAmounts(shopId, any(), any()) } returns emptyList()
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        val actual = shopSettlementService.calculateShopSettlement(shopId, march)

        assertEquals(0L, actual.carriedOverAmount)
    }

    @Test
    fun `이미 같은 기간의 정산이 있으면 예외가 발생한다`() {
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.SHOP, shopId, any(), any()) } returns
            Settlement.withId(1L, SettlementTargetType.SHOP, shopId, Instant.now(), Instant.now().plusSeconds(1))

        val exception = assertThrows<BusinessException> { shopSettlementService.calculateShopSettlement(shopId, march) }

        assertEquals(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `적용할 요율이 없으면 예외가 발생한다`() {
        every { commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(any(), any()) } returns null

        val exception = assertThrows<BusinessException> { shopSettlementService.calculateShopSettlement(shopId, march) }

        assertEquals(SettlementErrorCode.COMMISSION_RATE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `월 경계는 KST 기준 그 달 1일 00시부터 다음 달 1일 00시 직전까지다`() {
        val startSlot = slot<Instant>()
        val endSlot = slot<Instant>()
        every { orderService.getDeliveredOrderAmounts(shopId, capture(startSlot), capture(endSlot)) } returns emptyList()
        every { orderService.getRefundedPaymentAmounts(shopId, any(), any()) } returns emptyList()

        shopSettlementService.calculateShopSettlement(shopId, march)

        val (expectedStart, expectedEnd) = monthRangeFor(march)
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

        val actual = shopSettlementService.calculateShopSettlement(shopId, march)

        // 이번 달 요율은 20%지만, 과거 판매 때 적용됐던 10%를 그대로 써서 -9,000이 된다.
        assertEquals(-9_000L, actual.totalAmount)
        assertEquals(BigDecimal("0.1000"), itemsSlot.captured.single().appliedFeeRate)
    }

    private fun monthRangeFor(yearMonth: YearMonth): Pair<Instant, Instant> {
        val zone = java.time.ZoneId.of("Asia/Seoul")
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        return start to end
    }
}
