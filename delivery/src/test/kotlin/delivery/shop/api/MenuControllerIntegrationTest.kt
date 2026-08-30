package delivery.shop.api

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
    @Autowired private val shopService: ShopService,
    @Autowired private val menuService: MenuService,
) : IntegrationTestSupport() {

    private fun createMenu(): Long {
        val shop = shopService.create(
            CreateShopCommand(
                ownerId = System.nanoTime(),
                name = "가게",
                address = "서울",
                latitude = BigDecimal("37.5665000"),
                longitude = BigDecimal("126.9780000"),
                phone = "0212345678",
            )
        )
        val menuGroup = menuService.createMenuGroup(CreateMenuGroupCommand(shopId = shop.id!!, name = "메인", displayOrder = 0))
        val menu = menuService.createMenu(
            CreateMenuCommand(shopId = shop.id!!, menuGroupId = menuGroup.id!!, name = "짜장면", description = null, price = 8000L, displayOrder = 0)
        )
        return menu.id!!
    }

    @Test
    fun `이미지를 업로드하면 imageUrl을 반환하고 이후 조회할 수 있다`() {
        val menuId = createMenu()
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3, 4))

        mockMvc.perform(multipart("/menus/$menuId/image").file(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.menuId").value(menuId))
            .andExpect(jsonPath("$.imageUrl").exists())

        mockMvc.perform(get("/menus/$menuId/image"))
            .andExpect(status().isOk)
    }

    @Test
    fun `지원하지 않는 파일 형식을 업로드하면 400을 반환한다`() {
        val menuId = createMenu()
        val file = MockMultipartFile("file", "malware.exe", "application/octet-stream", byteArrayOf(1, 2, 3))

        mockMvc.perform(multipart("/menus/$menuId/image").file(file))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_MENU_IMAGE"))
    }

    @Test
    fun `존재하지 않는 메뉴에 이미지를 업로드하면 404를 반환한다`() {
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        mockMvc.perform(multipart("/menus/999999/image").file(file))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"))
    }

    @Test
    fun `이미지가 없는 메뉴를 조회하면 404를 반환한다`() {
        val menuId = createMenu()

        mockMvc.perform(get("/menus/$menuId/image"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MENU_IMAGE_NOT_FOUND"))
    }
}
