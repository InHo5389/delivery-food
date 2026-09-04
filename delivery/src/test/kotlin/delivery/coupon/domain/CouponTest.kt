package delivery.coupon.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CouponTest {

    private fun newCoupon(totalQuantity: Int = 100, validityDays: Int = 7, shopId: Long? = null): Coupon =
        Coupon(name = "치킨 할인 쿠폰", shopId = shopId, totalQuantity = totalQuantity, validityDays = validityDays)

    @Test
    fun `신규 생성 시 id는 null이다`() {
        assertNull(newCoupon().id)
    }

    @Test
    fun `신규 생성 시 발급수량은 0이다`() {
        assertEquals(0, newCoupon().issuedQuantity)
    }

    @Test
    fun `shopId가 null이면 플랫폼 전체 쿠폰이다`() {
        assertNull(newCoupon(shopId = null).shopId)
    }

    @Test
    fun `shopId를 지정하면 특정 상점 쿠폰이다`() {
        assertEquals(1L, newCoupon(shopId = 1L).shopId)
    }

    @Test
    fun `총수량이 1이면 생성에 성공한다`() {
        assertEquals(1, newCoupon(totalQuantity = 1).totalQuantity)
    }

    @Test
    fun `총수량이 0이면 생성에 실패한다`() {
        assertThrows<IllegalArgumentException> { newCoupon(totalQuantity = 0) }
    }

    @Test
    fun `총수량이 음수면 생성에 실패한다`() {
        assertThrows<IllegalArgumentException> { newCoupon(totalQuantity = -1) }
    }

    @Test
    fun `유효기간이 1일이면 생성에 성공한다`() {
        assertEquals(1, newCoupon(validityDays = 1).validityDays)
    }

    @Test
    fun `유효기간이 0일이면 생성에 실패한다`() {
        assertThrows<IllegalArgumentException> { newCoupon(validityDays = 0) }
    }

    @Test
    fun `유효기간이 음수면 생성에 실패한다`() {
        assertThrows<IllegalArgumentException> { newCoupon(validityDays = -1) }
    }

    @Test
    fun `발급수량을 늘리면 1 증가한다`() {
        val coupon = newCoupon()

        coupon.increaseIssuedQuantity()

        assertEquals(1, coupon.issuedQuantity)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val coupon = Coupon.withId(
            id = 10L,
            name = "치킨 할인 쿠폰",
            totalQuantity = 100,
            validityDays = 7,
            startsAt = Instant.parse("2026-03-01T00:00:00Z"),
        )

        assertEquals(10L, coupon.id)
    }
}
