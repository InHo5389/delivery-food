package delivery.auth.application

import delivery.auth.application.dto.TokenPair
import delivery.auth.domain.Account
import delivery.auth.domain.AuthErrorCode
import delivery.auth.domain.RefreshToken
import delivery.auth.domain.Role
import delivery.auth.infrastructure.AccountRepository
import delivery.auth.infrastructure.JwtProvider
import delivery.auth.infrastructure.RefreshTokenRepository
import delivery.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TokenServiceTest {

    private val jwtProvider = mockk<JwtProvider>()
    private val accountRepository = mockk<AccountRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()

    private lateinit var tokenService: TokenService

    @BeforeEach
    fun setUp() {
        tokenService = TokenService(jwtProvider, accountRepository, refreshTokenRepository)
    }

    @Test
    fun `토큰 쌍을 발급하면 access token은 JwtProvider에서 만들고 refresh token은 저장한다`() {
        every { jwtProvider.issueAccessToken(1L, Role.CUSTOMER) } returns "access-token"
        val savedSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val actual = tokenService.issueTokenPair(1L, Role.CUSTOMER)

        assertEquals("access-token", actual.accessToken)
        assertEquals(savedSlot.captured.token, actual.refreshToken)
        assertEquals(1L, savedSlot.captured.accountId)
    }

    @Test
    fun `연속 발급한 refresh token은 서로 다르다`() {
        every { jwtProvider.issueAccessToken(any(), any()) } returns "access-token"
        val saved = mutableListOf<RefreshToken>()
        every { refreshTokenRepository.save(any()) } answers {
            (it.invocation.args[0] as RefreshToken).also { rt -> saved.add(rt) }
        }

        tokenService.issueTokenPair(1L, Role.CUSTOMER)
        tokenService.issueTokenPair(1L, Role.CUSTOMER)

        assertNotEquals(saved[0].token, saved[1].token)
    }

    @Test
    fun `존재하지 않는 refresh token으로 갱신하면 실패한다`() {
        every { refreshTokenRepository.findByToken("unknown-token") } returns null

        val exception = assertThrows<BusinessException> { tokenService.refresh("unknown-token") }

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    fun `만료된 refresh token으로 갱신하면 실패한다`() {
        val expired = RefreshToken(
            accountId = 1L,
            token = "expired-token",
            expiresAt = Instant.now().minusSeconds(1),
        )
        every { refreshTokenRepository.findByToken("expired-token") } returns expired

        val exception = assertThrows<BusinessException> { tokenService.refresh("expired-token") }

        assertEquals(AuthErrorCode.REFRESH_TOKEN_EXPIRED, exception.errorCode)
    }

    @Test
    fun `유효한 refresh token으로 갱신하면 기존 토큰을 삭제하고 새 토큰 쌍을 발급한다`() {
        val valid = RefreshToken(
            accountId = 1L,
            token = "valid-token",
            expiresAt = Instant.now().plusSeconds(60),
        )
        val account = Account.withId(1L, "test@test.com", "encoded", "홍길동", "01012345678", Role.RIDER)

        every { refreshTokenRepository.findByToken("valid-token") } returns valid
        every { accountRepository.findById(1L) } returns Optional.of(account)
        every { refreshTokenRepository.delete(valid) } returns Unit
        every { jwtProvider.issueAccessToken(1L, Role.RIDER) } returns "new-access-token"
        val savedSlot = slot<RefreshToken>()
        every { refreshTokenRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val actual = tokenService.refresh("valid-token")

        assertEquals("new-access-token", actual.accessToken)
        assertTrue(actual.refreshToken.isNotBlank())
        verify(exactly = 1) { refreshTokenRepository.delete(valid) }
    }
}
