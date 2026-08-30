package delivery.shop.api

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
    @Autowired private val shopService: ShopService,
    @Autowired private val menuService: MenuService,
) : IntegrationTestSupport() {

    @Test
    fun `상점 상세를 조회하면 상점 정보와 메뉴가 함께 내려온다`() {
        val shop = shopService.create(
            CreateShopCommand(
                ownerId = System.nanoTime(),
                name = "테스트가게",
                address = "서울",
                latitude = BigDecimal("37.5665000"),
                longitude = BigDecimal("126.9780000"),
                phone = "0212345678",
            )
        )
        val menuGroup = menuService.createMenuGroup(CreateMenuGroupCommand(shopId = shop.id!!, name = "메인", displayOrder = 0))
        menuService.createMenu(
            CreateMenuCommand(
                shopId = shop.id!!,
                menuGroupId = menuGroup.id!!,
                name = "짜장면",
                description = "맛있는 짜장면",
                price = 8000L,
                displayOrder = 0,
            )
        )

        mockMvc.perform(get("/shops/${shop.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shopId").value(shop.id))
            .andExpect(jsonPath("$.name").value("테스트가게"))
            .andExpect(jsonPath("$.menuGroups[0].name").value("메인"))
            .andExpect(jsonPath("$.menuGroups[0].menus[0].name").value("짜장면"))
            .andExpect(jsonPath("$.menuGroups[0].menus[0].price").value(8000))
    }

    @Test
    fun `존재하지 않는 상점을 조회하면 404를 반환한다`() {
        mockMvc.perform(get("/shops/999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("SHOP_NOT_FOUND"))
    }

    @Test
    fun `메뉴가 없는 상점은 빈 메뉴그룹 목록을 반환한다`() {
        val shop = shopService.create(
            CreateShopCommand(
                ownerId = System.nanoTime(),
                name = "메뉴없는가게",
                address = "부산",
                latitude = BigDecimal("35.1796000"),
                longitude = BigDecimal("129.0756000"),
                phone = "0512345678",
            )
        )

        mockMvc.perform(get("/shops/${shop.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.menuGroups").isEmpty)
    }
}
