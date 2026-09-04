package delivery.coupon.infrastructure

import delivery.coupon.domain.Coupon
import delivery.coupon.domain.Issuance
import delivery.coupon.domain.IssuanceStatus
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IssuanceRepositoryIntegrationTest(
    @Autowired private val issuanceRepository: IssuanceRepository,
    @Autowired private val couponRepository: CouponRepository,
) : IntegrationTestSupport() {

    private fun newCoupon(): Coupon =
        couponRepository.save(Coupon(name = "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7))

    @Test
    fun `발급을 저장하면 id가 채번된다`() {
        val coupon = newCoupon()

        val issuance = issuanceRepository.save(Issuance(userId = 1L, couponId = coupon.id!!, validityDays = 7))

        assertEquals(IssuanceStatus.ISSUED, issuance.status)
    }

    @Test
    fun `같은 사용자가 같은 쿠폰을 두 번 발급받으면 유니크 제약 위반이 발생한다`() {
        val coupon = newCoupon()
        issuanceRepository.saveAndFlush(Issuance(userId = 1L, couponId = coupon.id!!, validityDays = 7))

        org.junit.jupiter.api.assertThrows<DataIntegrityViolationException> {
            issuanceRepository.saveAndFlush(Issuance(userId = 1L, couponId = coupon.id!!, validityDays = 7))
        }
    }

    @Test
    fun `같은 사용자가 다른 쿠폰을 발급받는 것은 허용된다`() {
        val coupon1 = newCoupon()
        val coupon2 = newCoupon()
        issuanceRepository.saveAndFlush(Issuance(userId = 1L, couponId = coupon1.id!!, validityDays = 7))

        issuanceRepository.saveAndFlush(Issuance(userId = 1L, couponId = coupon2.id!!, validityDays = 7))

        assertEquals(2, issuanceRepository.findAllByUserId(1L).size)
    }

    @Test
    fun `existsByUserIdAndCouponId는 이미 발급받은 경우 true를 반환한다`() {
        val coupon = newCoupon()
        issuanceRepository.save(Issuance(userId = 1L, couponId = coupon.id!!, validityDays = 7))

        assertTrue(issuanceRepository.existsByUserIdAndCouponId(1L, coupon.id!!))
    }

    @Test
    fun `existsByUserIdAndCouponId는 발급받은 적이 없으면 false를 반환한다`() {
        val coupon = newCoupon()

        assertFalse(issuanceRepository.existsByUserIdAndCouponId(1L, coupon.id!!))
    }

    @Test
    fun `findAllByCouponId는 그 쿠폰의 발급 건만 반환한다`() {
        val coupon1 = newCoupon()
        val coupon2 = newCoupon()
        issuanceRepository.save(Issuance(userId = 1L, couponId = coupon1.id!!, validityDays = 7))
        issuanceRepository.save(Issuance(userId = 2L, couponId = coupon1.id!!, validityDays = 7))
        issuanceRepository.save(Issuance(userId = 1L, couponId = coupon2.id!!, validityDays = 7))

        val actual = issuanceRepository.findAllByCouponId(coupon1.id!!)

        assertEquals(2, actual.size)
    }

    @Test
    fun `findAllByStatus는 그 상태의 발급 건만 반환한다`() {
        val coupon = newCoupon()
        val expired = issuanceRepository.save(Issuance(userId = 1L, couponId = coupon.id!!, validityDays = 7))
        expired.expire()
        issuanceRepository.save(expired)
        issuanceRepository.save(Issuance(userId = 2L, couponId = coupon.id!!, validityDays = 7))

        val actual = issuanceRepository.findAllByStatus(IssuanceStatus.EXPIRED)

        assertEquals(1, actual.size)
        assertEquals(IssuanceStatus.EXPIRED, actual.first().status)
    }

    @Test
    fun `발급 건이 없으면 findAllByUserId는 빈 목록을 반환한다`() {
        assertTrue(issuanceRepository.findAllByUserId(999L).isEmpty())
    }
}
