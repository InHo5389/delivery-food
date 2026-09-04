package delivery.coupon.infrastructure

import delivery.coupon.domain.Coupon
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CouponRepositoryIntegrationTest(
    @Autowired private val couponRepository: CouponRepository,
) : IntegrationTestSupport() {

    @Test
    fun `쿠폰을 저장하면 id가 채번된다`() {
        val coupon = couponRepository.save(Coupon(name = "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7))

        assertNotNull(coupon.id)
    }

    @Test
    fun `shopId가 null인 쿠폰도 저장할 수 있다`() {
        val coupon = couponRepository.save(
            Coupon(name = "플랫폼 전체 쿠폰", shopId = null, totalQuantity = 100, validityDays = 7)
        )

        val actual = couponRepository.findById(coupon.id!!).get()

        assertEquals(null, actual.shopId)
    }

    @Test
    fun `shopId가 있는 쿠폰을 저장하면 그대로 조회된다`() {
        val coupon = couponRepository.save(
            Coupon(name = "상점 전용 쿠폰", shopId = 1L, totalQuantity = 100, validityDays = 7)
        )

        val actual = couponRepository.findById(coupon.id!!).get()

        assertEquals(1L, actual.shopId)
    }

    @Test
    fun `발급수량이 증가한 상태로 저장하면 그대로 조회된다`() {
        val coupon = couponRepository.save(Coupon(name = "치킨 할인 쿠폰", totalQuantity = 100, validityDays = 7))
        coupon.increaseIssuedQuantity()
        couponRepository.save(coupon)

        val actual = couponRepository.findById(coupon.id!!).get()

        assertEquals(1, actual.issuedQuantity)
    }
}
