package delivery.auth.infrastructure

import delivery.auth.domain.Role
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtProviderTest {

    private lateinit var jwtProvider: JwtProvider

    @BeforeEach
    fun setUp() {
        jwtProvider = JwtProvider(secret = "test-secret-key-must-be-at-least-32-bytes-long")
    }

    @Test
    fun `발급한 토큰은 유효하다`() {
        val token = jwtProvider.issueAccessToken(1L, Role.CUSTOMER)

        assertTrue(jwtProvider.isValid(token))
    }

    @Test
    fun `발급한 토큰의 subject와 role 클레임이 정확하다`() {
        val token = jwtProvider.issueAccessToken(42L, Role.OWNER)

        val claims = jwtProvider.parseClaims(token)

        assertEquals("42", claims.subject)
        assertEquals("OWNER", claims["role"])
    }

    @Test
    fun `서명이 조작된 토큰은 유효하지 않다`() {
        val token = jwtProvider.issueAccessToken(1L, Role.CUSTOMER)
        val tampered = token.dropLast(2) + "xx"

        assertFalse(jwtProvider.isValid(tampered))
    }

    @Test
    fun `다른 secret으로 서명한 토큰은 유효하지 않다`() {
        val otherProvider = JwtProvider(secret = "another-secret-key-must-be-at-least-32-bytes")
        val token = otherProvider.issueAccessToken(1L, Role.CUSTOMER)

        assertFalse(jwtProvider.isValid(token))
    }

    @Test
    fun `형식이 올바르지 않은 토큰은 유효하지 않다`() {
        assertFalse(jwtProvider.isValid("not-a-jwt"))
    }
}
