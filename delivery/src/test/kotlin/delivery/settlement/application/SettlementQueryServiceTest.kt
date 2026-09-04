package delivery.settlement.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.delivery.application.DeliveryService
import delivery.settlement.application.dto.AdminSettlementRangeQuery
import delivery.settlement.application.dto.MySettlementQuery
import delivery.settlement.domain.CommissionRate
import delivery.settlement.domain.RateType
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementErrorCode
import delivery.settlement.domain.SettlementItem
import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementStatus
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.CommissionRateRepository
import delivery.settlement.infrastructure.SettlementItemRepository
import delivery.settlement.infrastructure.SettlementRepository
import delivery.shop.application.ShopService
import delivery.shop.domain.Shop
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class SettlementQueryServiceTest {

    private val settlementRepository = mockk<SettlementRepository>()
    private val settlementItemRepository = mockk<SettlementItemRepository>()
    private val commissionRateRepository = mockk<CommissionRateRepository>()
    private val shopService = mockk<ShopService>()
    private val deliveryService = mockk<DeliveryService>()
    private lateinit var settlementQueryService: SettlementQueryService

    private val shopId = 1L
    private val ownerId = 10L
    private val riderId = 2L
    private val riderAccountId = 20L
    private val zone = ZoneId.of("Asia/Seoul")

    // 2026-03-09는 월요일이다 — 상점 정산 기간(주)의 시작으로 쓴다.
    private val monday = LocalDate.of(2026, 3, 9)
    private val shopPeriodStart = monday.atStartOfDay(zone).toInstant()
    private val shopPeriodEnd = monday.plusWeeks(1).atStartOfDay(zone).toInstant()

    // 라이더 정산 기간(하루)은 같은 날짜 문자열로 조회하되 하루 범위로 계산된다.
    private val riderDate = monday
    private val riderPeriodStart = riderDate.atStartOfDay(zone).toInstant()
    private val riderPeriodEnd = riderDate.plusDays(1).atStartOfDay(zone).toInstant()

    @BeforeEach
    fun setUp() {
        settlementQueryService = SettlementQueryService(settlementRepository, settlementItemRepository, commissionRateRepository, shopService, deliveryService)
        every { commissionRateRepository.findTopByRateTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(any(), any()) } returns
            CommissionRate.withId(1L, RateType.PLATFORM_FEE, BigDecimal("0.2000"), Instant.EPOCH)
    }

    private fun shopSettlement(): Settlement =
        Settlement.withId(100L, SettlementTargetType.SHOP, shopId, shopPeriodStart, shopPeriodEnd, totalAmount = 16_000L)

    private fun riderSettlement(): Settlement =
        Settlement.withId(200L, SettlementTargetType.RIDER, riderId, riderPeriodStart, riderPeriodEnd, totalAmount = 3_000L)

    @Test
    fun `사장님이 shopId와 date로 본인 상점의 정산을 조회하면 그 주 정산을 반환한다`() {
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        val settlement = shopSettlement()
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.SHOP, shopId, shopPeriodStart, shopPeriodEnd) } returns settlement
        every { settlementItemRepository.findAllBySettlementId(100L) } returns listOf(
            SettlementItem.withId(1L, 100L, 101L, SettlementItemType.SALE, amount = 20_000L, appliedFeeRate = BigDecimal("0.2000"), settlementAmount = 16_000L)
        )

        val actual = settlementQueryService.getMySettlement(AuthenticatedUser(ownerId, Role.OWNER), MySettlementQuery(monday.toString(), shopId))

        assertEquals(1, actual.orderCount)
        assertEquals(20_000L, actual.grossAmount)
        assertEquals(16_000L, actual.payoutAmount)
    }

    @Test
    fun `사장님이 월요일이 아닌 날짜로 조회해도 그 주(월요일 시작)로 보정돼 조회된다`() {
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        val settlement = shopSettlement()
        val wednesday = monday.plusDays(2)
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.SHOP, shopId, shopPeriodStart, shopPeriodEnd) } returns settlement
        every { settlementItemRepository.findAllBySettlementId(100L) } returns emptyList()

        val actual = settlementQueryService.getMySettlement(AuthenticatedUser(ownerId, Role.OWNER), MySettlementQuery(wednesday.toString(), shopId))

        assertEquals(100L, actual.settlementId)
    }

    @Test
    fun `사장님이 shopId 없이 조회하면 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> {
            settlementQueryService.getMySettlement(AuthenticatedUser(ownerId, Role.OWNER), MySettlementQuery(monday.toString(), null))
        }

        assertEquals(SettlementErrorCode.SHOP_ID_REQUIRED, exception.errorCode)
    }

    @Test
    fun `다른 사람의 상점 정산을 조회하면 예외가 발생한다`() {
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            settlementQueryService.getMySettlement(AuthenticatedUser(999L, Role.OWNER), MySettlementQuery(monday.toString(), shopId))
        }

        assertEquals(SettlementErrorCode.NOT_SETTLEMENT_OWNER, exception.errorCode)
    }

    @Test
    fun `라이더가 본인 정산을 조회하면 accountId로 riderId를 찾아 그 날짜의 정산을 반환한다`() {
        every { deliveryService.getRiderIdByAccountId(riderAccountId) } returns riderId
        val settlement = riderSettlement()
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.RIDER, riderId, riderPeriodStart, riderPeriodEnd) } returns settlement
        every { settlementItemRepository.findAllBySettlementId(200L) } returns emptyList()

        val actual = settlementQueryService.getMySettlement(AuthenticatedUser(riderAccountId, Role.RIDER), MySettlementQuery(riderDate.toString(), null))

        assertEquals(SettlementTargetType.RIDER, actual.targetType)
        assertEquals(3_000L, actual.payoutAmount)
    }

    @Test
    fun `고객 역할로 조회하면 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> {
            settlementQueryService.getMySettlement(AuthenticatedUser(1L, Role.CUSTOMER), MySettlementQuery(monday.toString(), null))
        }

        assertEquals(SettlementErrorCode.NOT_SETTLEMENT_OWNER, exception.errorCode)
    }

    @Test
    fun `해당 기간의 정산이 없으면 예외가 발생한다`() {
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { settlementRepository.findByTargetTypeAndTargetIdAndPeriodStartAndPeriodEnd(SettlementTargetType.SHOP, shopId, shopPeriodStart, shopPeriodEnd) } returns null

        val exception = assertThrows<BusinessException> {
            settlementQueryService.getMySettlement(AuthenticatedUser(ownerId, Role.OWNER), MySettlementQuery(monday.toString(), shopId))
        }

        assertEquals(SettlementErrorCode.SETTLEMENT_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `날짜 형식이 잘못되면 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> {
            settlementQueryService.getMySettlement(AuthenticatedUser(ownerId, Role.OWNER), MySettlementQuery("2026년3월9일", shopId))
        }

        assertEquals(SettlementErrorCode.INVALID_DATE, exception.errorCode)
    }

    @Test
    fun `본인 소유 정산이면 항목 목록을 조회할 수 있다`() {
        every { settlementRepository.findById(100L) } returns java.util.Optional.of(shopSettlement())
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")
        every { settlementItemRepository.findAllBySettlementId(100L) } returns listOf(
            SettlementItem.withId(1L, 100L, 101L, SettlementItemType.SALE, amount = 20_000L, appliedFeeRate = BigDecimal("0.2000"), settlementAmount = 16_000L)
        )

        val actual = settlementQueryService.getSettlementItems(AuthenticatedUser(ownerId, Role.OWNER), 100L)

        assertEquals(1, actual.size)
    }

    @Test
    fun `다른 사람의 정산 항목을 조회하면 예외가 발생한다`() {
        every { settlementRepository.findById(100L) } returns java.util.Optional.of(shopSettlement())
        every { shopService.getById(shopId) } returns Shop.withId(shopId, ownerId, "가게", "서울", "0212345678")

        val exception = assertThrows<BusinessException> {
            settlementQueryService.getSettlementItems(AuthenticatedUser(999L, Role.OWNER), 100L)
        }

        assertEquals(SettlementErrorCode.NOT_SETTLEMENT_OWNER, exception.errorCode)
    }

    @Test
    fun `운영자는 소유자가 아니어도 정산 항목을 조회할 수 있다`() {
        every { settlementRepository.findById(100L) } returns java.util.Optional.of(shopSettlement())
        every { settlementItemRepository.findAllBySettlementId(100L) } returns emptyList()

        val actual = settlementQueryService.getSettlementItems(AuthenticatedUser(999L, Role.ADMIN), 100L)

        assertEquals(0, actual.size)
    }

    @Test
    fun `존재하지 않는 정산 항목을 조회하면 예외가 발생한다`() {
        every { settlementRepository.findById(999L) } returns java.util.Optional.empty()

        val exception = assertThrows<BusinessException> {
            settlementQueryService.getSettlementItems(AuthenticatedUser(ownerId, Role.OWNER), 999L)
        }

        assertEquals(SettlementErrorCode.SETTLEMENT_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `운영자가 아니면 전체 정산 목록을 조회할 수 없다`() {
        val exception = assertThrows<BusinessException> {
            settlementQueryService.getAdminSettlements(AuthenticatedUser(ownerId, Role.OWNER), AdminSettlementRangeQuery("2026-03-01", "2026-03-15"))
        }

        assertEquals(SettlementErrorCode.NOT_ADMIN, exception.errorCode)
    }

    @Test
    fun `운영자는 기간 범위의 전체 정산 목록을 조회할 수 있다`() {
        every {
            settlementRepository.findAllByPeriodStartGreaterThanEqualAndPeriodStartLessThanOrderByPeriodStartDesc(any(), any())
        } returns listOf(shopSettlement())
        every { settlementItemRepository.findAllBySettlementId(100L) } returns emptyList()

        val actual = settlementQueryService.getAdminSettlements(AuthenticatedUser(999L, Role.ADMIN), AdminSettlementRangeQuery("2026-03-01", "2026-03-15"))

        assertEquals(1, actual.size)
    }

    @Test
    fun `운영자가 아니면 정산을 확정할 수 없다`() {
        val exception = assertThrows<BusinessException> {
            settlementQueryService.confirmSettlement(AuthenticatedUser(ownerId, Role.OWNER), 100L)
        }

        assertEquals(SettlementErrorCode.NOT_ADMIN, exception.errorCode)
    }

    @Test
    fun `운영자가 정산을 확정하면 CONFIRMED로 바뀐다`() {
        val settlement = shopSettlement()
        every { settlementRepository.findById(100L) } returns java.util.Optional.of(settlement)
        every { settlementItemRepository.findAllBySettlementId(100L) } returns emptyList()

        val actual = settlementQueryService.confirmSettlement(AuthenticatedUser(999L, Role.ADMIN), 100L)

        assertEquals(SettlementStatus.CONFIRMED, actual.status)
    }

    @Test
    fun `운영자가 정산을 지급 완료 처리하면 PAID로 바뀐다`() {
        val settlement = shopSettlement()
        settlement.transitionTo(SettlementStatus.CONFIRMED)
        every { settlementRepository.findById(100L) } returns java.util.Optional.of(settlement)
        every { settlementItemRepository.findAllBySettlementId(100L) } returns emptyList()

        val actual = settlementQueryService.paySettlement(AuthenticatedUser(999L, Role.ADMIN), 100L)

        assertEquals(SettlementStatus.PAID, actual.status)
    }

    @Test
    fun `PENDING 상태에서 바로 PAID로 지급 처리하면 예외가 발생한다`() {
        val settlement = shopSettlement()
        every { settlementRepository.findById(100L) } returns java.util.Optional.of(settlement)

        val exception = assertThrows<BusinessException> {
            settlementQueryService.paySettlement(AuthenticatedUser(999L, Role.ADMIN), 100L)
        }

        assertEquals(SettlementErrorCode.INVALID_SETTLEMENT_STATUS_TRANSITION, exception.errorCode)
    }
}
