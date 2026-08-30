package delivery.auth.application

import delivery.auth.application.dto.LoginCommand
import delivery.auth.application.dto.SignupCommand
import delivery.auth.application.dto.TokenPair
import delivery.auth.domain.Account
import delivery.auth.domain.AuthErrorCode
import delivery.auth.domain.Role
import delivery.auth.infrastructure.AccountRepository
import delivery.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals

class AuthServiceTest {

    private val accountRepository = mockk<AccountRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val tokenService = mockk<TokenService>()

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(accountRepository, passwordEncoder, tokenService)
    }

    @Test
    fun `이미 가입된 이메일로 회원가입하면 실패한다`() {
        val command = SignupCommand("existing@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)
        every { accountRepository.existsByEmail(command.email) } returns true

        val exception = assertThrows<BusinessException> { authService.signup(command) }

        assertEquals(AuthErrorCode.EMAIL_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `신규 이메일로 회원가입하면 계정을 저장하고 토큰을 발급한다`() {
        val command = SignupCommand("new@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)
        val savedAccount = Account.withId(1L, command.email, "encoded", command.name, command.phone, command.role)
        val tokenPair = TokenPair("access-token", "refresh-token")
        val accountSlot = slot<Account>()

        every { accountRepository.existsByEmail(command.email) } returns false
        every { passwordEncoder.encode(command.password) } returns "encoded"
        every { accountRepository.save(capture(accountSlot)) } returns savedAccount
        every { tokenService.issueTokenPair(1L, Role.CUSTOMER) } returns tokenPair

        val actual = authService.signup(command)

        assertEquals(tokenPair, actual)
        assertEquals("encoded", accountSlot.captured.password)
        verify(exactly = 1) { tokenService.issueTokenPair(1L, Role.CUSTOMER) }
    }

    @Test
    fun `존재하지 않는 이메일로 로그인하면 실패한다`() {
        val command = LoginCommand("unknown@test.com", "password1234")
        every { accountRepository.findByEmail(command.email) } returns null

        val exception = assertThrows<BusinessException> { authService.login(command) }

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, exception.errorCode)
    }

    @Test
    fun `비밀번호가 일치하지 않으면 로그인에 실패한다`() {
        val command = LoginCommand("existing@test.com", "wrong-password")
        val account = Account.withId(1L, command.email, "encoded", "홍길동", "01012345678", Role.CUSTOMER)
        every { accountRepository.findByEmail(command.email) } returns account
        every { passwordEncoder.matches(command.password, "encoded") } returns false

        val exception = assertThrows<BusinessException> { authService.login(command) }

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, exception.errorCode)
    }

    @Test
    fun `이메일과 비밀번호가 일치하면 토큰을 발급한다`() {
        val command = LoginCommand("existing@test.com", "password1234")
        val account = Account.withId(1L, command.email, "encoded", "홍길동", "01012345678", Role.OWNER)
        val tokenPair = TokenPair("access-token", "refresh-token")
        every { accountRepository.findByEmail(command.email) } returns account
        every { passwordEncoder.matches(command.password, "encoded") } returns true
        every { tokenService.issueTokenPair(1L, Role.OWNER) } returns tokenPair

        val actual = authService.login(command)

        assertEquals(tokenPair, actual)
    }
}
