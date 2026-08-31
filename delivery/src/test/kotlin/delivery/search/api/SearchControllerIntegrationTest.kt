package delivery.search.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import delivery.common.security.AuthenticatedUser
import delivery.shop.application.ShopService
import delivery.shop.application.dto.CreateShopCommand
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class SearchControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val shopService: ShopService,
) : IntegrationTestSupport() {

    private fun signupOwner(email: String): AuthenticatedUser {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "사장님", phone = "01011112222", role = Role.OWNER)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return AuthenticatedUser(userId, Role.OWNER)
    }

    @Test
    fun `키워드로 상점을 검색하면 200과 결과를 반환한다`() {
        val owner = signupOwner("search-owner1@test.com")
        val shop = shopService.create(
            CreateShopCommand(
                name = "굽네치킨 서초점",
                address = "서울",
                latitude = BigDecimal("37.5665000"),
                longitude = BigDecimal("126.9780000"),
                phone = "0212345678",
                minOrderAmount = 12000,
                deliveryFee = 3000,
            ),
            owner,
        )
        shopService.open(shop.id!!, owner)

        mockMvc.perform(get("/search/shops").param("keyword", "굽네"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("굽네치킨 서초점"))
            .andExpect(jsonPath("$[0].minOrderAmount").value(12000))
            .andExpect(jsonPath("$[0].deliveryFee").value(3000))
    }

    @Test
    fun `키워드가 비어있으면 400을 반환한다`() {
        mockMvc.perform(get("/search/shops").param("keyword", ""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `키워드 파라미터가 없으면 400을 반환한다`() {
        mockMvc.perform(get("/search/shops"))
            .andExpect(status().isBadRequest)
    }
}
