package delivery.shop.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import delivery.common.security.AuthenticatedUser
import delivery.shop.application.MenuService
import delivery.shop.application.ShopService
import delivery.shop.application.dto.CreateMenuCommand
import delivery.shop.application.dto.CreateMenuGroupCommand
import delivery.shop.application.dto.CreateShopCommand
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class ShopControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val shopService: ShopService,
    @Autowired private val menuService: MenuService,
) : IntegrationTestSupport() {

    private fun signupOwner(email: String): AuthenticatedUser {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "사장님", phone = "01011112222", role = Role.OWNER)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return AuthenticatedUser(userId, Role.OWNER)
    }

    @Test
    fun `상점 상세를 조회하면 상점 정보와 메뉴가 함께 내려온다`() {
        val owner = signupOwner("shopdetail1@test.com")
        val shop = shopService.create(
            CreateShopCommand(
                name = "테스트가게",
                address = "서울",
                latitude = BigDecimal("37.5665000"),
                longitude = BigDecimal("126.9780000"),
                phone = "0212345678",
                minOrderAmount = 10000,
                deliveryFee = 2500,
            ),
            owner,
        )
        val menuGroup = menuService.createMenuGroup(CreateMenuGroupCommand(shopId = shop.id!!, name = "메인", displayOrder = 0), owner)
        menuService.createMenu(
            CreateMenuCommand(
                shopId = shop.id!!,
                menuGroupId = menuGroup.id!!,
                name = "짜장면",
                description = "맛있는 짜장면",
                price = 8000L,
                displayOrder = 0,
            ),
            owner,
        )

        mockMvc.perform(get("/shops/${shop.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shopId").value(shop.id))
            .andExpect(jsonPath("$.name").value("테스트가게"))
            .andExpect(jsonPath("$.minOrderAmount").value(10000))
            .andExpect(jsonPath("$.deliveryFee").value(2500))
            .andExpect(jsonPath("$.menuGroups[0].name").value("메인"))
            .andExpect(jsonPath("$.menuGroups[0].menus[0].name").value("짜장면"))
            .andExpect(jsonPath("$.menuGroups[0].menus[0].price").value(8000))
    }

    @Test
    fun `근처 상점 목록을 조회하면 최소주문금액과 배달비가 함께 내려온다`() {
        val owner = signupOwner("shopdetail3@test.com")
        val shop = shopService.create(
            CreateShopCommand(
                name = "근처가게",
                address = "서울",
                latitude = BigDecimal("37.5665000"),
                longitude = BigDecimal("126.9780000"),
                phone = "0212345678",
                minOrderAmount = 8000,
                deliveryFee = 2000,
            ),
            owner,
        )
        shopService.open(shop.id!!, owner)

        mockMvc.perform(get("/shops").param("latitude", "37.5665").param("longitude", "126.9780"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.shopId == ${shop.id})].minOrderAmount").value(8000))
            .andExpect(jsonPath("$[?(@.shopId == ${shop.id})].deliveryFee").value(2000))
    }

    @Test
    fun `기본 반경(3km) 밖의 상점은 근처 목록에서 제외된다`() {
        val owner = signupOwner("shopdetail4@test.com")
        val farShop = shopService.create(
            CreateShopCommand(
                name = "부산가게",
                address = "부산",
                latitude = BigDecimal("35.1796000"),
                longitude = BigDecimal("129.0756000"),
                phone = "0512345678",
                minOrderAmount = 0,
                deliveryFee = 0,
            ),
            owner,
        )
        shopService.open(farShop.id!!, owner)

        mockMvc.perform(get("/shops").param("latitude", "37.5665").param("longitude", "126.9780"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.shopId == ${farShop.id})]").isEmpty)
    }

    @Test
    fun `존재하지 않는 상점을 조회하면 404를 반환한다`() {
        mockMvc.perform(get("/shops/999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("SHOP_NOT_FOUND"))
    }

    @Test
    fun `메뉴가 없는 상점은 빈 메뉴그룹 목록을 반환한다`() {
        val owner = signupOwner("shopdetail2@test.com")
        val shop = shopService.create(
            CreateShopCommand(
                name = "메뉴없는가게",
                address = "부산",
                latitude = BigDecimal("35.1796000"),
                longitude = BigDecimal("129.0756000"),
                phone = "0512345678",
                minOrderAmount = 0,
                deliveryFee = 0,
            ),
            owner,
        )

        mockMvc.perform(get("/shops/${shop.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.menuGroups").isEmpty)
    }
}
