package delivery.shop.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import delivery.common.security.AuthenticatedUser
import delivery.shop.application.MenuService
import delivery.shop.application.ShopService
import delivery.shop.application.dto.CreateShopCommand
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
import java.math.BigDecimal

class MenuManagementControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val shopService: ShopService,
    @Autowired private val menuService: MenuService,
) : IntegrationTestSupport() {

    private data class SignedUpOwner(val user: AuthenticatedUser, val token: String)

    private fun signupOwner(email: String): SignedUpOwner {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "사장님", phone = "01011112222", role = Role.OWNER)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return SignedUpOwner(AuthenticatedUser(userId, Role.OWNER), tokenPair.accessToken)
    }

    private fun createShopId(owner: AuthenticatedUser): Long =
        shopService.create(
            CreateShopCommand(
                name = "가게",
                address = "서울",
                latitude = BigDecimal("37.5665000"),
                longitude = BigDecimal("126.9780000"),
                phone = "0212345678",
                minOrderAmount = 0,
                deliveryFee = 0,
            ),
            owner,
        ).id!!

    @Test
    fun `사장님이 메뉴 그룹을 생성하면 201을 반환한다`() {
        val owner = signupOwner("menugroup-owner1@test.com")
        val shopId = createShopId(owner.user)
        val body = """{"name":"메인","displayOrder":0}"""

        mockMvc.perform(post("/shops/$shopId/menu-groups").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("메인"))
    }

    @Test
    fun `다른 사람의 상점에 메뉴 그룹을 생성하면 403을 반환한다`() {
        val owner = signupOwner("menugroup-owner2@test.com")
        val shopId = createShopId(owner.user)
        val other = signupOwner("menugroup-owner3@test.com")
        val body = """{"name":"메인","displayOrder":0}"""

        mockMvc.perform(post("/shops/$shopId/menu-groups").header("Authorization", "Bearer ${other.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_SHOP_OWNER"))
    }

    @Test
    fun `메뉴 그룹 이름이 빈 값이면 400을 반환한다`() {
        val owner = signupOwner("menugroup-owner4@test.com")
        val shopId = createShopId(owner.user)
        val body = """{"name":"","displayOrder":0}"""

        mockMvc.perform(post("/shops/$shopId/menu-groups").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `사장님이 메뉴를 생성하면 201을 반환한다`() {
        val owner = signupOwner("menumgmt-owner1@test.com")
        val shopId = createShopId(owner.user)
        val menuGroup = menuService.createMenuGroup(
            delivery.shop.application.dto.CreateMenuGroupCommand(shopId = shopId, name = "메인", displayOrder = 0),
            owner.user,
        )
        val body = """{"menuGroupId":${menuGroup.id},"name":"짜장면","description":"맛있는 짜장면","price":8000,"displayOrder":0}"""

        mockMvc.perform(post("/shops/$shopId/menus").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("짜장면"))
            .andExpect(jsonPath("$.price").value(8000))
    }

    @Test
    fun `사장님이 메뉴를 일괄 생성하면 201과 전체 목록을 반환한다`() {
        val owner = signupOwner("menumgmt-bulk1@test.com")
        val shopId = createShopId(owner.user)
        val menuGroup = menuService.createMenuGroup(
            delivery.shop.application.dto.CreateMenuGroupCommand(shopId = shopId, name = "메인", displayOrder = 0),
            owner.user,
        )
        val body = """
            {"menus":[
                {"menuGroupId":${menuGroup.id},"name":"짜장면","description":null,"price":8000,"displayOrder":0},
                {"menuGroupId":${menuGroup.id},"name":"짬뽕","description":null,"price":9000,"displayOrder":1}
            ]}
        """.trimIndent()

        mockMvc.perform(post("/shops/$shopId/menus/bulk").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("짜장면"))
            .andExpect(jsonPath("$[1].name").value("짬뽕"))
    }

    @Test
    fun `빈 목록으로 일괄 생성하면 400을 반환한다`() {
        val owner = signupOwner("menumgmt-bulk2@test.com")
        val shopId = createShopId(owner.user)
        val body = """{"menus":[]}"""

        mockMvc.perform(post("/shops/$shopId/menus/bulk").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `다른 사람의 상점에 메뉴를 일괄 생성하면 403을 반환한다`() {
        val owner = signupOwner("menumgmt-bulk3@test.com")
        val shopId = createShopId(owner.user)
        val menuGroup = menuService.createMenuGroup(
            delivery.shop.application.dto.CreateMenuGroupCommand(shopId = shopId, name = "메인", displayOrder = 0),
            owner.user,
        )
        val other = signupOwner("menumgmt-bulk4@test.com")
        val body = """{"menus":[{"menuGroupId":${menuGroup.id},"name":"짜장면","description":null,"price":8000,"displayOrder":0}]}"""

        mockMvc.perform(post("/shops/$shopId/menus/bulk").header("Authorization", "Bearer ${other.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_SHOP_OWNER"))
    }

    @Test
    fun `존재하지 않는 메뉴 그룹으로 일괄 생성하면 404를 반환한다`() {
        val owner = signupOwner("menumgmt-bulk5@test.com")
        val shopId = createShopId(owner.user)
        val body = """{"menus":[{"menuGroupId":999999,"name":"짜장면","description":null,"price":8000,"displayOrder":0}]}"""

        mockMvc.perform(post("/shops/$shopId/menus/bulk").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MENU_GROUP_NOT_FOUND"))
    }

    @Test
    fun `가격이 0 이하이면 메뉴 생성 시 400을 반환한다`() {
        val owner = signupOwner("menumgmt-owner2@test.com")
        val shopId = createShopId(owner.user)
        val menuGroup = menuService.createMenuGroup(
            delivery.shop.application.dto.CreateMenuGroupCommand(shopId = shopId, name = "메인", displayOrder = 0),
            owner.user,
        )
        val body = """{"menuGroupId":${menuGroup.id},"name":"짜장면","description":null,"price":0,"displayOrder":0}"""

        mockMvc.perform(post("/shops/$shopId/menus").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `메뉴를 품절 처리하면 200을 반환한다`() {
        val owner = signupOwner("menumgmt-owner3@test.com")
        val shopId = createShopId(owner.user)
        val menuGroup = menuService.createMenuGroup(
            delivery.shop.application.dto.CreateMenuGroupCommand(shopId = shopId, name = "메인", displayOrder = 0),
            owner.user,
        )
        val menu = menuService.createMenu(
            delivery.shop.application.dto.CreateMenuCommand(shopId, menuGroup.id!!, "짜장면", null, 8000L, 0),
            owner.user,
        )

        mockMvc.perform(post("/menus/${menu.id}/sold-out").header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isOk)
    }

    @Test
    fun `다른 사람의 메뉴를 품절 처리하면 403을 반환한다`() {
        val owner = signupOwner("menumgmt-owner4@test.com")
        val shopId = createShopId(owner.user)
        val menuGroup = menuService.createMenuGroup(
            delivery.shop.application.dto.CreateMenuGroupCommand(shopId = shopId, name = "메인", displayOrder = 0),
            owner.user,
        )
        val menu = menuService.createMenu(
            delivery.shop.application.dto.CreateMenuCommand(shopId, menuGroup.id!!, "짜장면", null, 8000L, 0),
            owner.user,
        )
        val other = signupOwner("menumgmt-owner5@test.com")

        mockMvc.perform(post("/menus/${menu.id}/sold-out").header("Authorization", "Bearer ${other.token}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `메뉴를 삭제하면 204를 반환한다`() {
        val owner = signupOwner("menumgmt-owner6@test.com")
        val shopId = createShopId(owner.user)
        val menuGroup = menuService.createMenuGroup(
            delivery.shop.application.dto.CreateMenuGroupCommand(shopId = shopId, name = "메인", displayOrder = 0),
            owner.user,
        )
        val menu = menuService.createMenu(
            delivery.shop.application.dto.CreateMenuCommand(shopId, menuGroup.id!!, "짜장면", null, 8000L, 0),
            owner.user,
        )

        mockMvc.perform(delete("/menus/${menu.id}").header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `존재하지 않는 메뉴를 수정하면 404를 반환한다`() {
        val owner = signupOwner("menumgmt-owner7@test.com")
        val body = """{"name":"짬뽕","description":null,"price":9000,"displayOrder":0}"""

        mockMvc.perform(put("/menus/999999").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound)
    }
}
