package delivery.auth.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import kotlin.test.assertEquals

class RefreshTokenTest {

    @Test
    fun `만료 시각 이전이면 만료되지 않은 것으로 판단한다`() {
        val expiresAt = Instant.parse("2026-01-01T00:00:00Z")
        val token = RefreshToken(accountId = 1L, token = "token", expiresAt = expiresAt)

        val actual = token.isExpired(now = expiresAt.minusSeconds(1))

        assertEquals(false, actual)
    }

    @Test
    fun `만료 시각과 정확히 같으면 만료되지 않은 것으로 판단한다`() {
        val expiresAt = Instant.parse("2026-01-01T00:00:00Z")
        val token = RefreshToken(accountId = 1L, token = "token", expiresAt = expiresAt)

        val actual = token.isExpired(now = expiresAt)

        assertEquals(false, actual)
    }

    @Test
    fun `만료 시각 이후면 만료된 것으로 판단한다`() {
        val expiresAt = Instant.parse("2026-01-01T00:00:00Z")
        val token = RefreshToken(accountId = 1L, token = "token", expiresAt = expiresAt)

        val actual = token.isExpired(now = expiresAt.plusSeconds(1))

        assertEquals(true, actual)
    }

    @ParameterizedTest
    @CsvSource(
        "-1, false",
        "0, false",
        "1, true",
    )
    fun `만료 시각 기준 경계값 오프셋별 만료 여부`(offsetSeconds: Long, expected: Boolean) {
        val expiresAt = Instant.parse("2026-01-01T00:00:00Z")
        val token = RefreshToken(accountId = 1L, token = "token", expiresAt = expiresAt)

        val actual = token.isExpired(now = expiresAt.plusSeconds(offsetSeconds))

        assertEquals(expected, actual)
    }
}
