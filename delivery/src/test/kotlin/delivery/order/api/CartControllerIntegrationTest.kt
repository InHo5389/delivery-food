package delivery.order.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CartControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
) : IntegrationTestSupport() {

    private fun issueCustomerToken(email: String): String {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "고객", phone = "01011112222", role = Role.CUSTOMER)
        )
        return tokenPair.accessToken
    }

    @Test
    fun `메뉴를 담으면 201과 장바구니 내용을 반환한다`() {
        val token = issueCustomerToken("cart-customer1@test.com")
        val body = """{"shopId":1,"menuId":1,"menuName":"짜장면","menuPrice":8000,"quantity":2}"""

        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.totalPrice").value(16000))
    }

    @Test
    fun `수량이 0이면 400을 반환한다`() {
        val token = issueCustomerToken("cart-customer2@test.com")
        val body = """{"shopId":1,"menuId":1,"menuName":"짜장면","menuPrice":8000,"quantity":0}"""

        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `다른 상점의 메뉴를 담으면 409를 반환한다`() {
        val token = issueCustomerToken("cart-customer3@test.com")
        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"shopId":1,"menuId":1,"menuName":"짜장면","menuPrice":8000,"quantity":1}"""))
            .andExpect(status().isCreated)

        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"shopId":2,"menuId":2,"menuName":"초밥","menuPrice":15000,"quantity":1}"""))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("DIFFERENT_SHOP_IN_CART"))
    }

    @Test
    fun `장바구니를 조회한다`() {
        val token = issueCustomerToken("cart-customer4@test.com")
        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"shopId":1,"menuId":1,"menuName":"짜장면","menuPrice":8000,"quantity":1}"""))

        mockMvc.perform(get("/cart").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].menuName").value("짜장면"))
    }

    @Test
    fun `존재하지 않는 장바구니를 조회하면 404를 반환한다`() {
        val token = issueCustomerToken("cart-customer5@test.com")

        mockMvc.perform(get("/cart").header("Authorization", "Bearer $token"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `항목 수량을 변경한다`() {
        val token = issueCustomerToken("cart-customer6@test.com")
        val response = mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"shopId":1,"menuId":1,"menuName":"짜장면","menuPrice":8000,"quantity":1}"""))
            .andReturn()
        val cartItemId = Regex("\"cartItemId\":(\\d+)").find(response.response.contentAsString)!!.groupValues[1]

        mockMvc.perform(put("/cart/items/$cartItemId").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"quantity":5}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].quantity").value(5))
    }

    @Test
    fun `항목을 삭제한다`() {
        val token = issueCustomerToken("cart-customer7@test.com")
        val response = mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"shopId":1,"menuId":1,"menuName":"짜장면","menuPrice":8000,"quantity":1}"""))
            .andReturn()
        val cartItemId = Regex("\"cartItemId\":(\\d+)").find(response.response.contentAsString)!!.groupValues[1]

        mockMvc.perform(delete("/cart/items/$cartItemId").header("Authorization", "Bearer $token"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `장바구니를 비운다`() {
        val token = issueCustomerToken("cart-customer8@test.com")
        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content("""{"shopId":1,"menuId":1,"menuName":"짜장면","menuPrice":8000,"quantity":1}"""))

        mockMvc.perform(delete("/cart").header("Authorization", "Bearer $token"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `토큰 없이 장바구니에 담으면 인증 오류를 반환한다`() {
        val body = """{"shopId":1,"menuId":1,"menuName":"짜장면","menuPrice":8000,"quantity":1}"""

        mockMvc.perform(post("/cart/items").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is4xxClientError)
    }
}
