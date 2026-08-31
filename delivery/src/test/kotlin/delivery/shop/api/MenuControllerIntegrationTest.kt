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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class MenuControllerIntegrationTest(
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

    private fun createMenu(owner: AuthenticatedUser): Long {
        val shop = shopService.create(
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
        )
        val menuGroup = menuService.createMenuGroup(CreateMenuGroupCommand(shopId = shop.id!!, name = "메인", displayOrder = 0), owner)
        val menu = menuService.createMenu(
            CreateMenuCommand(shopId = shop.id!!, menuGroupId = menuGroup.id!!, name = "짜장면", description = null, price = 8000L, displayOrder = 0),
            owner,
        )
        return menu.id!!
    }

    @Test
    fun `상점 사장님이 이미지를 업로드하면 imageUrl을 반환하고 이후 조회할 수 있다`() {
        val owner = signupOwner("menu-owner1@test.com")
        val menuId = createMenu(owner.user)
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3, 4))

        mockMvc.perform(multipart("/menus/$menuId/image").file(file).header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.menuId").value(menuId))
            .andExpect(jsonPath("$.imageUrl").exists())

        mockMvc.perform(get("/menus/$menuId/image"))
            .andExpect(status().isOk)
    }

    @Test
    fun `다른 사람의 상점 메뉴에 업로드하면 403을 반환한다`() {
        val owner = signupOwner("menu-owner2@test.com")
        val menuId = createMenu(owner.user)
        val other = signupOwner("menu-owner3@test.com")
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3, 4))

        mockMvc.perform(multipart("/menus/$menuId/image").file(file).header("Authorization", "Bearer ${other.token}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_SHOP_OWNER"))
    }

    @Test
    fun `토큰 없이 업로드하면 인증 오류를 반환한다`() {
        val owner = signupOwner("menu-owner4@test.com")
        val menuId = createMenu(owner.user)
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3, 4))

        mockMvc.perform(multipart("/menus/$menuId/image").file(file))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `지원하지 않는 파일 형식을 업로드하면 400을 반환한다`() {
        val owner = signupOwner("menu-owner5@test.com")
        val menuId = createMenu(owner.user)
        val file = MockMultipartFile("file", "malware.exe", "application/octet-stream", byteArrayOf(1, 2, 3))

        mockMvc.perform(multipart("/menus/$menuId/image").file(file).header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_MENU_IMAGE"))
    }

    @Test
    fun `존재하지 않는 메뉴에 이미지를 업로드하면 404를 반환한다`() {
        val owner = signupOwner("menu-owner6@test.com")
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        mockMvc.perform(multipart("/menus/999999/image").file(file).header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"))
    }

    @Test
    fun `이미지가 없는 메뉴를 조회하면 404를 반환한다`() {
        val owner = signupOwner("menu-owner7@test.com")
        val menuId = createMenu(owner.user)

        mockMvc.perform(get("/menus/$menuId/image"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MENU_IMAGE_NOT_FOUND"))
    }
}
