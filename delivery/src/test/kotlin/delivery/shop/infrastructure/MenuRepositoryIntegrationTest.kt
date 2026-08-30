package delivery.shop.infrastructure

import delivery.shop.domain.Menu
import delivery.shop.domain.MenuGroup
import delivery.shop.domain.MenuOption
import delivery.shop.domain.MenuOptionGroup
import delivery.shop.domain.Shop
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MenuRepositoryIntegrationTest(
    @Autowired private val shopRepository: ShopRepository,
    @Autowired private val menuGroupRepository: MenuGroupRepository,
    @Autowired private val menuRepository: MenuRepository,
    @Autowired private val menuOptionGroupRepository: MenuOptionGroupRepository,
    @Autowired private val menuOptionRepository: MenuOptionRepository,
) : IntegrationTestSupport() {

    @Test
    fun `상점부터 메뉴 옵션까지 전체 계층을 저장하고 조회한다`() {
        val shop = shopRepository.save(Shop(ownerId = 1L, name = "가게", address = "서울", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0212345678"))
        val menuGroup = menuGroupRepository.save(MenuGroup(shopId = shop.id!!, name = "메인", displayOrder = 0))
        val menu = menuRepository.save(
            Menu(shopId = shop.id!!, menuGroupId = menuGroup.id!!, name = "짜장면", price = 8000L, displayOrder = 0)
        )
        val optionGroup = menuOptionGroupRepository.save(
            MenuOptionGroup(menuId = menu.id!!, name = "곱빼기 선택", required = false, displayOrder = 0)
        )
        menuOptionRepository.save(MenuOption(menuOptionGroupId = optionGroup.id!!, name = "곱빼기", price = 2000L, displayOrder = 0))

        val menuGroups = menuGroupRepository.findAllByShopIdOrderByDisplayOrder(shop.id!!)
        val menus = menuRepository.findAllByShopIdOrderByDisplayOrder(shop.id!!)
        val optionGroups = menuOptionGroupRepository.findAllByMenuIdOrderByDisplayOrder(menu.id!!)
        val options = menuOptionRepository.findAllByMenuOptionGroupIdOrderByDisplayOrder(optionGroup.id!!)

        assertEquals(1, menuGroups.size)
        assertEquals(1, menus.size)
        assertEquals("짜장면", menus[0].name)
        assertEquals(1, optionGroups.size)
        assertEquals(1, options.size)
        assertEquals(2000L, options[0].price)
    }

    @Test
    fun `메뉴 그룹별로 메뉴 목록을 조회한다`() {
        val shop = shopRepository.save(Shop(ownerId = 2L, name = "가게2", address = "부산", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0511111111"))
        val group1 = menuGroupRepository.save(MenuGroup(shopId = shop.id!!, name = "메인", displayOrder = 0))
        val group2 = menuGroupRepository.save(MenuGroup(shopId = shop.id!!, name = "사이드", displayOrder = 1))
        menuRepository.save(Menu(shopId = shop.id!!, menuGroupId = group1.id!!, name = "짜장면", price = 8000L, displayOrder = 0))
        menuRepository.save(Menu(shopId = shop.id!!, menuGroupId = group2.id!!, name = "탕수육", price = 15000L, displayOrder = 0))

        val group1Menus = menuRepository.findAllByMenuGroupIdOrderByDisplayOrder(group1.id!!)

        assertEquals(1, group1Menus.size)
        assertEquals("짜장면", group1Menus[0].name)
    }

    @Test
    fun `기본값으로 저장한 메뉴는 품절 상태가 아니다`() {
        val shop = shopRepository.save(Shop(ownerId = 3L, name = "가게3", address = "인천", latitude = java.math.BigDecimal("37.5665000"), longitude = java.math.BigDecimal("126.9780000"), phone = "0322222222"))
        val menuGroup = menuGroupRepository.save(MenuGroup(shopId = shop.id!!, name = "메인", displayOrder = 0))
        val menu = menuRepository.save(
            Menu(shopId = shop.id!!, menuGroupId = menuGroup.id!!, name = "짜장면", price = 8000L, displayOrder = 0)
        )

        val actual = menuRepository.findById(menu.id!!).get()

        assertTrue(!actual.soldOut)
    }
}
