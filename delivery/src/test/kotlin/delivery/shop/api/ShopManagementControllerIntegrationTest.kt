package delivery.shop.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ShopManagementControllerIntegrationTest(
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

    private val createBody = """
        {"name":"가게","address":"서울","latitude":37.5665,"longitude":126.9780,"phone":"0212345678","minOrderAmount":12000,"deliveryFee":3000}
    """.trimIndent()

    private fun createShop(token: String): Long {
        val response = mockMvc.perform(
            post("/shops").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(createBody)
        ).andExpect(status().isCreated).andReturn()
        val json = response.response.contentAsString
        return Regex("\"shopId\":(\\d+)").find(json)!!.groupValues[1].toLong()
    }

    @Test
    fun `사장님이 상점을 생성하면 201과 결과를 반환한다`() {
        val token = issueOwnerToken("shop-owner1@test.com")

        mockMvc.perform(
            post("/shops").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(createBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("가게"))
            .andExpect(jsonPath("$.status").value("CLOSED"))
            .andExpect(jsonPath("$.minOrderAmount").value(12000))
            .andExpect(jsonPath("$.deliveryFee").value(3000))
    }

    @Test
    fun `토큰 없이 상점을 생성하면 인증 오류를 반환한다`() {
        mockMvc.perform(post("/shops").contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `고객 역할로 상점을 생성하면 403을 반환한다`() {
        val token = issueCustomerToken("shop-customer1@test.com")

        mockMvc.perform(
            post("/shops").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(createBody)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `이름이 빈 값이면 상점 생성 시 400을 반환한다`() {
        val token = issueOwnerToken("shop-owner2@test.com")
        val body = """{"name":"","address":"서울","latitude":37.5665,"longitude":126.9780,"phone":"0212345678","minOrderAmount":0,"deliveryFee":0}"""

        mockMvc.perform(post("/shops").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `위도가 범위를 벗어나면 400을 반환한다`() {
        val token = issueOwnerToken("shop-owner3@test.com")
        val body = """{"name":"가게","address":"서울","latitude":91.0,"longitude":126.9780,"phone":"0212345678","minOrderAmount":0,"deliveryFee":0}"""

        mockMvc.perform(post("/shops").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `최소주문금액이 음수이면 400을 반환한다`() {
        val token = issueOwnerToken("shop-owner12@test.com")
        val body = """{"name":"가게","address":"서울","latitude":37.5665,"longitude":126.9780,"phone":"0212345678","minOrderAmount":-1,"deliveryFee":0}"""

        mockMvc.perform(post("/shops").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `배달비가 음수이면 400을 반환한다`() {
        val token = issueOwnerToken("shop-owner13@test.com")
        val body = """{"name":"가게","address":"서울","latitude":37.5665,"longitude":126.9780,"phone":"0212345678","minOrderAmount":0,"deliveryFee":-1}"""

        mockMvc.perform(post("/shops").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `최소주문금액과 배달비가 0이면 정상 생성된다`() {
        val token = issueOwnerToken("shop-owner14@test.com")
        val body = """{"name":"가게","address":"서울","latitude":37.5665,"longitude":126.9780,"phone":"0212345678","minOrderAmount":0,"deliveryFee":0}"""

        mockMvc.perform(post("/shops").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.minOrderAmount").value(0))
            .andExpect(jsonPath("$.deliveryFee").value(0))
    }

    @Test
    fun `자기 상점을 수정하면 반영된 결과를 반환한다`() {
        val token = issueOwnerToken("shop-owner4@test.com")
        val shopId = createShop(token)
        val updateBody = """{"name":"새이름","address":"부산","latitude":35.1796,"longitude":129.0756,"phone":"0511111111","minOrderAmount":15000,"deliveryFee":4000}"""

        mockMvc.perform(put("/shops/$shopId").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("새이름"))
            .andExpect(jsonPath("$.minOrderAmount").value(15000))
            .andExpect(jsonPath("$.deliveryFee").value(4000))
    }

    @Test
    fun `다른 사람의 상점을 수정하면 403을 반환한다`() {
        val token = issueOwnerToken("shop-owner5@test.com")
        val shopId = createShop(token)
        val otherToken = issueOwnerToken("shop-owner6@test.com")
        val updateBody = """{"name":"새이름","address":"부산","latitude":35.1796,"longitude":129.0756,"phone":"0511111111","minOrderAmount":0,"deliveryFee":0}"""

        mockMvc.perform(put("/shops/$shopId").header("Authorization", "Bearer $otherToken").contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_SHOP_OWNER"))
    }

    @Test
    fun `존재하지 않는 상점을 수정하면 404를 반환한다`() {
        val token = issueOwnerToken("shop-owner7@test.com")
        val updateBody = """{"name":"새이름","address":"부산","latitude":35.1796,"longitude":129.0756,"phone":"0511111111","minOrderAmount":0,"deliveryFee":0}"""

        mockMvc.perform(put("/shops/999999").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `상점을 오픈하면 200을 반환한다`() {
        val token = issueOwnerToken("shop-owner8@test.com")
        val shopId = createShop(token)

        mockMvc.perform(post("/shops/$shopId/open").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
    }

    @Test
    fun `다른 사람의 상점을 오픈하면 403을 반환한다`() {
        val token = issueOwnerToken("shop-owner9@test.com")
        val shopId = createShop(token)
        val otherToken = issueOwnerToken("shop-owner10@test.com")

        mockMvc.perform(post("/shops/$shopId/open").header("Authorization", "Bearer $otherToken"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `상점을 삭제하면 204를 반환한다`() {
        val token = issueOwnerToken("shop-owner11@test.com")
        val shopId = createShop(token)

        mockMvc.perform(delete("/shops/$shopId").header("Authorization", "Bearer $token"))
            .andExpect(status().isNoContent)
    }
}
