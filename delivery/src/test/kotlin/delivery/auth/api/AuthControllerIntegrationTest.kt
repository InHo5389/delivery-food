package delivery.auth.api

import tools.jackson.databind.ObjectMapper
import delivery.auth.api.dto.LoginRequest
import delivery.auth.api.dto.RefreshRequest
import delivery.auth.api.dto.SignupRequest
import delivery.auth.api.dto.TokenResponse
import delivery.auth.domain.Role
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) : IntegrationTestSupport() {

    @Test
    fun `회원가입에 성공하면 토큰 쌍을 반환한다`() {
        val request = SignupRequest("signup-success@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)

        mockMvc.post("/auth/signup", request)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
    }

    @Test
    fun `이미 가입된 이메일로 회원가입하면 409를 반환한다`() {
        val request = SignupRequest("duplicate@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)
        mockMvc.post("/auth/signup", request).andExpect(status().isOk)

        mockMvc.post("/auth/signup", request)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
    }

    @Test
    fun `이메일 형식이 올바르지 않으면 400을 반환한다`() {
        val request = SignupRequest("invalid-email", "password1234", "홍길동", "01012345678", Role.CUSTOMER)

        mockMvc.post("/auth/signup", request)
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `가입한 계정으로 로그인하면 토큰 쌍을 반환한다`() {
        val signupRequest = SignupRequest("login-success@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)
        mockMvc.post("/auth/signup", signupRequest).andExpect(status().isOk)

        val loginRequest = LoginRequest(signupRequest.email, signupRequest.password)

        mockMvc.post("/auth/login", loginRequest)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
    }

    @Test
    fun `존재하지 않는 이메일로 로그인하면 401을 반환한다`() {
        val request = LoginRequest("not-exists@test.com", "password1234")

        mockMvc.post("/auth/login", request)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
    }

    @Test
    fun `비밀번호가 틀리면 401을 반환한다`() {
        val signupRequest = SignupRequest("wrong-password@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)
        mockMvc.post("/auth/signup", signupRequest).andExpect(status().isOk)

        val loginRequest = LoginRequest(signupRequest.email, "wrong-password")

        mockMvc.post("/auth/login", loginRequest)
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `발급받은 refresh token으로 갱신하면 새 토큰 쌍을 반환한다`() {
        val signupRequest = SignupRequest("refresh-success@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)
        val signupResult = mockMvc.post("/auth/signup", signupRequest).andReturn()
        val tokenResponse = objectMapper.readValue(signupResult.response.contentAsString, TokenResponse::class.java)

        val refreshRequest = RefreshRequest(tokenResponse.refreshToken)

        mockMvc.post("/auth/refresh", refreshRequest)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.not(tokenResponse.refreshToken)))
    }

    @Test
    fun `존재하지 않는 refresh token으로 갱신하면 401을 반환한다`() {
        val request = RefreshRequest("non-existent-token")

        mockMvc.post("/auth/refresh", request)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
    }

    @Test
    fun `이미 사용한 refresh token으로 다시 갱신하면 401을 반환한다`() {
        val signupRequest = SignupRequest("reuse-token@test.com", "password1234", "홍길동", "01012345678", Role.CUSTOMER)
        val signupResult = mockMvc.post("/auth/signup", signupRequest).andReturn()
        val tokenResponse = objectMapper.readValue(signupResult.response.contentAsString, TokenResponse::class.java)
        val refreshRequest = RefreshRequest(tokenResponse.refreshToken)
        mockMvc.post("/auth/refresh", refreshRequest).andExpect(status().isOk)

        mockMvc.post("/auth/refresh", refreshRequest)
            .andExpect(status().isUnauthorized)
    }

    private fun MockMvc.post(uri: String, body: Any) =
        perform(
            post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
}
