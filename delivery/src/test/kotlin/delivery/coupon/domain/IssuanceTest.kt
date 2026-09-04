package delivery.coupon.domain

import delivery.common.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IssuanceTest {

    private val issuedAt: Instant = Instant.parse("2026-03-01T00:00:00Z")

    private fun newIssuance(validityDays: Int = 7): Issuance =
        Issuance(userId = 1L, couponId = 1L, issuedAt = issuedAt, validityDays = validityDays)

    @Test
    fun `신규 생성 시 id는 null이다`() {
        assertNull(newIssuance().id)
    }

    @Test
    fun `신규 생성 시 기본 상태는 ISSUED이다`() {
        assertEquals(IssuanceStatus.ISSUED, newIssuance().status)
    }

    @Test
    fun `신규 생성 시 사용시각은 null이다`() {
        assertNull(newIssuance().usedAt)
    }

    @Test
    fun `만료시각은 발급시각에 유효기간 일수를 더한 값이다`() {
        val issuance = newIssuance(validityDays = 7)

        assertEquals(issuedAt.plus(7, ChronoUnit.DAYS), issuance.expiresAt)
    }

    @Test
    fun `유효기간이 1일이면 만료시각은 발급시각의 다음날이다`() {
        val issuance = newIssuance(validityDays = 1)

        assertEquals(issuedAt.plus(1, ChronoUnit.DAYS), issuance.expiresAt)
    }

    @Test
    fun `사용하면 상태가 USED로 바뀌고 사용시각이 기록된다`() {
        val issuance = newIssuance()

        issuance.use()

        assertEquals(IssuanceStatus.USED, issuance.status)
        assertEquals(true, issuance.usedAt != null)
    }

    @Test
    fun `이미 사용된 발급은 다시 사용할 수 없다`() {
        val issuance = newIssuance()
        issuance.use()

        val exception = assertThrows<BusinessException> { issuance.use() }

        assertEquals(CouponErrorCode.INVALID_ISSUANCE_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `만료시키면 상태가 EXPIRED로 바뀐다`() {
        val issuance = newIssuance()

        issuance.expire()

        assertEquals(IssuanceStatus.EXPIRED, issuance.status)
    }

    @Test
    fun `이미 사용된 발급은 만료시킬 수 없다`() {
        val issuance = newIssuance()
        issuance.use()

        val exception = assertThrows<BusinessException> { issuance.expire() }

        assertEquals(CouponErrorCode.INVALID_ISSUANCE_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `이미 만료된 발급은 사용할 수 없다`() {
        val issuance = newIssuance()
        issuance.expire()

        val exception = assertThrows<BusinessException> { issuance.use() }

        assertEquals(CouponErrorCode.INVALID_ISSUANCE_STATUS_TRANSITION, exception.errorCode)
    }

    @Test
    fun `withId로 생성하면 id가 채번된 것처럼 세팅된다`() {
        val issuance = Issuance.withId(
            id = 10L,
            userId = 1L,
            couponId = 1L,
            issuedAt = issuedAt,
            validityDays = 7,
        )

        assertEquals(10L, issuance.id)
    }

    @Test
    fun `withId로 USED 상태와 사용시각을 지정할 수 있다`() {
        val usedAt = issuedAt.plus(1, ChronoUnit.DAYS)

        val issuance = Issuance.withId(
            id = 10L,
            userId = 1L,
            couponId = 1L,
            issuedAt = issuedAt,
            validityDays = 7,
            status = IssuanceStatus.USED,
            usedAt = usedAt,
        )

        assertEquals(IssuanceStatus.USED, issuance.status)
        assertEquals(usedAt, issuance.usedAt)
    }
}
