package delivery.auth.api.dto

import delivery.auth.domain.Role
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthDtoValidationTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `필드가 모두 유효하면 위반이 없다`() {
        val request = SignupRequest("test@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)

        val violations = validator.validate(request)

        assertTrue(violations.isEmpty())
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" ", "  "])
    fun `이메일이 비어있거나 공백이면 검증에 실패한다`(email: String?) {
        val request = SignupRequest(email ?: "", "password1234", "홍길동", "01012345678", Role.CUSTOMER)

        val violations = validator.validate(request)

        assertTrue(violations.isNotEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["not-an-email", "missing-at-sign.com", "@missing-local.com"])
    fun `이메일 형식이 아니면 검증에 실패한다`(email: String) {
        val request = SignupRequest(email, "password1234", "홍길동", "01012345678", Role.CUSTOMER)

        val violations = validator.validate(request)

        assertTrue(violations.isNotEmpty())
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" "])
    fun `비밀번호가 비어있거나 공백이면 검증에 실패한다`(password: String?) {
        val request = SignupRequest("test@test.com", password ?: "", "홍길동", "01012345678", Role.CUSTOMER)

        val violations = validator.validate(request)

        assertTrue(violations.isNotEmpty())
    }

    @Test
    fun `로그인 요청도 이메일과 비밀번호 검증을 동일하게 적용한다`() {
        val invalid = LoginRequest("invalid-email", "")

        val violations = validator.validate(invalid)

        assertEquals(2, violations.size)
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" "])
    fun `refresh token이 비어있거나 공백이면 검증에 실패한다`(token: String?) {
        val request = RefreshRequest(token ?: "")

        val violations = validator.validate(request)

        assertTrue(violations.isNotEmpty())
    }
}
