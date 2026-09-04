package delivery.coupon.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.coupon.application.dto.CreateCouponCommand
import delivery.coupon.domain.Coupon
import delivery.coupon.domain.CouponErrorCode
import delivery.coupon.domain.Issuance
import delivery.coupon.domain.IssuanceStatus
import delivery.coupon.infrastructure.CouponRepository
import delivery.coupon.infrastructure.IssuanceRepository
import delivery.shop.application.ShopService
import delivery.shop.domain.Shop
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals

class CouponServiceTest {

    private val couponRepository = mockk<CouponRepository>()
    private val issuanceRepository = mockk<IssuanceRepository>()
    private val shopService = mockk<ShopService>()
    private lateinit var couponService: CouponService

    private val shopId = 1L
    private val ownerId = 10L

    @BeforeEach
    fun setUp() {
        couponService = CouponService(couponRepository, issuanceRepository, shopService)
    }

    private fun newShop(): Shop =
        Shop.withId(id = shopId, ownerId = ownerId, name = "가게", address = "서울", phone = "0212345678")

    private fun newCommand(shopId: Long? = null, totalQuantity: Int = 100, validityDays: Int = 7, startsAt: Instant? = null): CreateCouponCommand =
        CreateCouponCommand("치킨 할인 쿠폰", shopId, totalQuantity, validityDays, startsAt)

    @Test
    fun `ADMIN은 shopId 없이 쿠폰을 생성할 수 있다`() {
        val savedSlot = slot<Coupon>()
        every { couponRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val coupon = couponService.createCoupon(AuthenticatedUser(1L, Role.ADMIN), newCommand(shopId = null))

        assertEquals(null, coupon.shopId)
    }

    @Test
    fun `ADMIN은 임의 상점의 쿠폰도 생성할 수 있다`() {
        val savedSlot = slot<Coupon>()
        every { couponRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val coupon = couponService.createCoupon(AuthenticatedUser(1L, Role.ADMIN), newCommand(shopId = shopId))

        assertEquals(shopId, coupon.shopId)
    }

    @Test
    fun `OWNER가 shopId 없이 생성하면 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> {
            couponService.createCoupon(AuthenticatedUser(ownerId, Role.OWNER), newCommand(shopId = null))
        }

        assertEquals(CouponErrorCode.SHOP_ID_REQUIRED, exception.errorCode)
    }

    @Test
    fun `OWNER는 본인 상점의 쿠폰을 생성할 수 있다`() {
        every { shopService.getById(shopId) } returns newShop()
        val savedSlot = slot<Coupon>()
        every { couponRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val coupon = couponService.createCoupon(AuthenticatedUser(ownerId, Role.OWNER), newCommand(shopId = shopId))

        assertEquals(shopId, coupon.shopId)
    }

    @Test
    fun `OWNER가 본인 소유가 아닌 상점의 쿠폰을 생성하려 하면 예외가 발생한다`() {
        every { shopService.getById(shopId) } returns newShop()

        val exception = assertThrows<BusinessException> {
            couponService.createCoupon(AuthenticatedUser(999L, Role.OWNER), newCommand(shopId = shopId))
        }

        assertEquals(CouponErrorCode.NOT_OWNER, exception.errorCode)
    }

    @Test
    fun `CUSTOMER는 쿠폰을 생성할 수 없다`() {
        val exception = assertThrows<BusinessException> {
            couponService.createCoupon(AuthenticatedUser(1L, Role.CUSTOMER), newCommand(shopId = shopId))
        }

        assertEquals(CouponErrorCode.NOT_OWNER, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 쿠폰을 발급하려 하면 예외가 발생한다`() {
        every { couponRepository.findByIdForUpdate(999L) } returns null

        val exception = assertThrows<BusinessException> { couponService.issue(999L, 1L) }

        assertEquals(CouponErrorCode.COUPON_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `시작 전 쿠폰을 발급하려 하면 예외가 발생한다`() {
        val coupon = Coupon.withId(1L, "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7, startsAt = Instant.now().plusSeconds(3600))
        every { couponRepository.findByIdForUpdate(1L) } returns coupon

        val exception = assertThrows<BusinessException> { couponService.issue(1L, 1L) }

        assertEquals(CouponErrorCode.NOT_STARTED, exception.errorCode)
    }

    @Test
    fun `이미 발급을 시작한 쿠폰(startsAt이 과거)은 발급할 수 있다`() {
        val coupon = Coupon.withId(1L, "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7, startsAt = Instant.now().minusSeconds(3600))
        every { couponRepository.findByIdForUpdate(1L) } returns coupon
        every { issuanceRepository.save(any()) } answers { firstArg() }

        val issuance = couponService.issue(1L, 1L)

        assertEquals(1L, issuance.couponId)
    }

    @Test
    fun `startsAt이 null이면 언제든 발급할 수 있다`() {
        val coupon = Coupon.withId(1L, "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7, startsAt = null)
        every { couponRepository.findByIdForUpdate(1L) } returns coupon
        every { issuanceRepository.save(any()) } answers { firstArg() }

        val issuance = couponService.issue(1L, 1L)

        assertEquals(1L, issuance.couponId)
    }

    @Test
    fun `매진된 쿠폰을 발급하려 하면 예외가 발생한다`() {
        val coupon = Coupon.withId(1L, "치킨 할인 쿠폰", totalQuantity = 100, issuedQuantity = 100, validityDays = 7)
        every { couponRepository.findByIdForUpdate(1L) } returns coupon

        val exception = assertThrows<BusinessException> { couponService.issue(1L, 1L) }

        assertEquals(CouponErrorCode.SOLD_OUT, exception.errorCode)
    }

    @Test
    fun `재고가 1개 남은 쿠폰은 발급에 성공하고 발급수량이 증가한다`() {
        val coupon = Coupon.withId(1L, "치킨 할인 쿠폰", totalQuantity = 100, issuedQuantity = 99, validityDays = 7)
        every { couponRepository.findByIdForUpdate(1L) } returns coupon
        every { issuanceRepository.save(any()) } answers { firstArg() }

        couponService.issue(1L, 1L)

        assertEquals(100, coupon.issuedQuantity)
    }

    @Test
    fun `이미 발급받은 쿠폰을 다시 발급받으려 하면 예외가 발생한다`() {
        val coupon = Coupon.withId(1L, "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7)
        every { couponRepository.findByIdForUpdate(1L) } returns coupon
        every { issuanceRepository.save(any()) } throws DataIntegrityViolationException("duplicate")

        val exception = assertThrows<BusinessException> { couponService.issue(1L, 1L) }

        assertEquals(CouponErrorCode.ALREADY_ISSUED, exception.errorCode)
    }

    @Test
    fun `내 발급 목록을 조회하면 그 사용자의 발급 건만 반환한다`() {
        val issuance = Issuance.withId(1L, userId = 1L, couponId = 1L, issuedAt = Instant.now(), validityDays = 7)
        every { issuanceRepository.findAllByUserId(1L) } returns listOf(issuance)

        val actual = couponService.getMyIssuances(1L)

        assertEquals(listOf(issuance), actual)
        verify(exactly = 1) { issuanceRepository.findAllByUserId(1L) }
    }

    @Test
    fun `존재하지 않는 발급 건을 사용하려 하면 예외가 발생한다`() {
        every { issuanceRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { couponService.use(999L, 1L) }

        assertEquals(CouponErrorCode.ISSUANCE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `남의 발급 건을 사용하려 하면 예외가 발생한다`() {
        val issuance = Issuance.withId(1L, userId = 1L, couponId = 1L, issuedAt = Instant.now(), validityDays = 7)
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance)

        val exception = assertThrows<BusinessException> { couponService.use(1L, 999L) }

        assertEquals(CouponErrorCode.NOT_OWNER, exception.errorCode)
    }

    @Test
    fun `이미 사용한 발급 건을 다시 사용하려 하면 예외가 발생한다`() {
        val issuance = Issuance.withId(1L, userId = 1L, couponId = 1L, issuedAt = Instant.now(), validityDays = 7, status = IssuanceStatus.USED)
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance)

        val exception = assertThrows<BusinessException> { couponService.use(1L, 1L) }

        assertEquals(CouponErrorCode.ALREADY_USED, exception.errorCode)
    }

    @Test
    fun `상태가 EXPIRED인 발급 건을 사용하려 하면 예외가 발생한다`() {
        val issuance = Issuance.withId(1L, userId = 1L, couponId = 1L, issuedAt = Instant.now().minusSeconds(1_000_000), validityDays = 7, status = IssuanceStatus.EXPIRED)
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance)

        val exception = assertThrows<BusinessException> { couponService.use(1L, 1L) }

        assertEquals(CouponErrorCode.EXPIRED, exception.errorCode)
    }

    @Test
    fun `상태는 ISSUED이지만 유효기간이 지났으면 사용할 수 없다`() {
        val issuedAt = Instant.now().minusSeconds(10 * 86400)
        val issuance = Issuance.withId(1L, userId = 1L, couponId = 1L, issuedAt = issuedAt, validityDays = 7)
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance)

        val exception = assertThrows<BusinessException> { couponService.use(1L, 1L) }

        assertEquals(CouponErrorCode.EXPIRED, exception.errorCode)
    }

    @Test
    fun `유효한 발급 건을 사용하면 상태가 USED로 바뀌고 사용시각이 기록된다`() {
        val issuance = Issuance.withId(1L, userId = 1L, couponId = 1L, issuedAt = Instant.now(), validityDays = 7)
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance)

        val actual = couponService.use(1L, 1L)

        assertEquals(IssuanceStatus.USED, actual.status)
        assertEquals(true, actual.usedAt != null)
    }

    @Test
    fun `발급 건을 만료시키면 상태가 EXPIRED로 바뀐다`() {
        val issuance = Issuance.withId(1L, userId = 1L, couponId = 1L, issuedAt = Instant.now().minusSeconds(10 * 86400), validityDays = 7)
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance)

        val actual = couponService.expireIssuance(1L)

        assertEquals(IssuanceStatus.EXPIRED, actual.status)
    }

    @Test
    fun `존재하지 않는 발급 건을 만료시키려 하면 예외가 발생한다`() {
        every { issuanceRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { couponService.expireIssuance(999L) }

        assertEquals(CouponErrorCode.ISSUANCE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `이미 사용된 발급 건을 만료시키려 하면 예외가 발생한다`() {
        val issuance = Issuance.withId(1L, userId = 1L, couponId = 1L, issuedAt = Instant.now(), validityDays = 7, status = IssuanceStatus.USED)
        every { issuanceRepository.findById(1L) } returns Optional.of(issuance)

        val exception = assertThrows<BusinessException> { couponService.expireIssuance(1L) }

        assertEquals(CouponErrorCode.INVALID_ISSUANCE_STATUS_TRANSITION, exception.errorCode)
    }
}
