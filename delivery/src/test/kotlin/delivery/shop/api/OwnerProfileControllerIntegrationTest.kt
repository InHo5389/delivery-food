package delivery.shop.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class OwnerProfileControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
) : IntegrationTestSupport() {

    private fun issueOwnerToken(email: String): String {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "사장님", phone = "01011112222", role = Role.OWNER)
        )
        return tokenPair.accessToken
    }

    private fun issueCustomerToken(email: String): String {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "고객", phone = "01033334444", role = Role.CUSTOMER)
        )
        return tokenPair.accessToken
    }

    @Test
    fun `사장님이 로그인 후 프로필을 생성하면 201 대신 200과 결과를 반환한다`() {
        val token = issueOwnerToken("owner1@test.com")
        val body = """
            {"businessRegistrationNumber":"123-45-67890","businessName":"가게상호","settlementBank":"국민은행","settlementAccount":"1234-56-789"}
        """.trimIndent()

        mockMvc.perform(
            post("/owner-profile")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.businessName").value("가게상호"))
    }

    @Test
    fun `토큰 없이 프로필을 생성하면 401 또는 403을 반환한다`() {
        val body = """
            {"businessRegistrationNumber":"123-45-67890","businessName":"가게상호","settlementBank":"국민은행","settlementAccount":"1234-56-789"}
        """.trimIndent()

        mockMvc.perform(
            post("/owner-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `고객 역할 토큰으로 프로필을 생성하면 403을 반환한다`() {
        val token = issueCustomerToken("customer1@test.com")
        val body = """
            {"businessRegistrationNumber":"123-45-67890","businessName":"가게상호","settlementBank":"국민은행","settlementAccount":"1234-56-789"}
        """.trimIndent()

        mockMvc.perform(
            post("/owner-profile")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `자기 프로필을 조회하면 생성한 내용을 반환한다`() {
        val token = issueOwnerToken("owner2@test.com")
        val body = """
            {"businessRegistrationNumber":"111-11-11111","businessName":"두번째가게","settlementBank":"우리은행","settlementAccount":"1111-11-111"}
        """.trimIndent()
        mockMvc.perform(
            post("/owner-profile")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)

        mockMvc.perform(get("/owner-profile").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.businessName").value("두번째가게"))
    }

    @Test
    fun `이미 프로필이 있는 사장님이 다시 생성하면 409를 반환한다`() {
        val token = issueOwnerToken("owner3@test.com")
        val body = """
            {"businessRegistrationNumber":"222-22-22222","businessName":"세번째가게","settlementBank":"하나은행","settlementAccount":"2222-22-222"}
        """.trimIndent()
        mockMvc.perform(
            post("/owner-profile")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/owner-profile")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("OWNER_PROFILE_ALREADY_EXISTS"))
    }
}
