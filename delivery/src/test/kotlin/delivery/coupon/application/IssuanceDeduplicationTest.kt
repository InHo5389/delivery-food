package delivery.coupon.application

import delivery.common.exception.BusinessException
import delivery.coupon.domain.CouponErrorCode
import delivery.coupon.domain.Issuance
import delivery.coupon.infrastructure.IssuanceRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import kotlin.test.assertEquals

class IssuanceDeduplicationTest {

    private val issuanceRepository = mockk<IssuanceRepository>()

    private fun newIssuance(): Issuance = Issuance(userId = 1L, couponId = 1L, issuedAt = Instant.now(), validityDays = 7)

    @Test
    fun `유니크 제약 위반 없이 저장되면 그대로 반환한다`() {
        val issuance = newIssuance()
        every { issuanceRepository.save(issuance) } returns issuance

        val actual = issuanceRepository.saveOrThrowDuplicate(issuance)

        assertEquals(issuance, actual)
    }

    @Test
    fun `유니크 제약 위반이면 ALREADY_ISSUED 예외로 변환한다`() {
        val issuance = newIssuance()
        every { issuanceRepository.save(issuance) } throws DataIntegrityViolationException("duplicate")

        val exception = assertThrows<BusinessException> { issuanceRepository.saveOrThrowDuplicate(issuance) }

        assertEquals(CouponErrorCode.ALREADY_ISSUED, exception.errorCode)
    }
}
