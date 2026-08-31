package delivery.shop.application

import delivery.auth.domain.Role
import delivery.common.exception.BusinessException
import delivery.common.security.AuthenticatedUser
import delivery.shop.application.dto.BulkCreateMenuCommand
import delivery.shop.application.dto.CreateMenuCommand
import delivery.shop.application.dto.CreateMenuGroupCommand
import delivery.shop.application.dto.CreateMenuItem
import delivery.shop.application.dto.UpdateMenuCommand
import delivery.shop.domain.Menu
import delivery.shop.domain.MenuGroup
import delivery.shop.domain.Shop
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.MenuGroupRepository
import delivery.shop.infrastructure.MenuImageStorage
import delivery.shop.infrastructure.MenuRepository
import delivery.shop.infrastructure.ShopRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.nio.file.Path
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuServiceTest {

    private val menuGroupRepository = mockk<MenuGroupRepository>()
    private val menuRepository = mockk<MenuRepository>()
    private val menuImageStorage = mockk<MenuImageStorage>()
    private val shopRepository = mockk<ShopRepository>()
    private lateinit var menuService: MenuService

    private val owner = AuthenticatedUser(userId = 1L, role = Role.OWNER)

    @BeforeEach
    fun setUp() {
        menuService = MenuService(menuGroupRepository, menuRepository, menuImageStorage, shopRepository)
    }

    @Test
    fun `메뉴 그룹을 생성하면 저장한다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        val command = CreateMenuGroupCommand(shopId = 1L, name = "메인", displayOrder = 0)
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        every { menuGroupRepository.save(any()) } answers { it.invocation.args[0] as delivery.shop.domain.MenuGroup }

        val actual = menuService.createMenuGroup(command, owner)

        assertEquals("메인", actual.name)
    }

    @Test
    fun `다른 사람의 상점에 메뉴 그룹을 생성하면 예외가 발생한다`() {
        val shop = Shop.withId(1L, 999L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        val command = CreateMenuGroupCommand(shopId = 1L, name = "메인", displayOrder = 0)
        every { shopRepository.findById(1L) } returns Optional.of(shop)

        val exception = assertThrows<BusinessException> { menuService.createMenuGroup(command, owner) }

        assertEquals(ShopErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `메뉴를 생성하면 저장한다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        val menuGroup = MenuGroup.withId(1L, 1L, "메인", 0)
        val command = CreateMenuCommand(shopId = 1L, menuGroupId = 1L, name = "짜장면", description = null, price = 8000L, displayOrder = 0)
        val savedSlot = slot<Menu>()
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        every { menuGroupRepository.findById(1L) } returns Optional.of(menuGroup)
        every { menuRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val actual = menuService.createMenu(command, owner)

        assertEquals("짜장면", actual.name)
        assertEquals(8000L, savedSlot.captured.price)
    }

    @Test
    fun `다른 상점 소속 메뉴 그룹에 메뉴를 생성하면 예외가 발생한다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        val menuGroup = MenuGroup.withId(1L, 2L, "다른가게메뉴", 0)
        val command = CreateMenuCommand(shopId = 1L, menuGroupId = 1L, name = "짜장면", description = null, price = 8000L, displayOrder = 0)
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        every { menuGroupRepository.findById(1L) } returns Optional.of(menuGroup)

        val exception = assertThrows<BusinessException> { menuService.createMenu(command, owner) }

        assertEquals(ShopErrorCode.MENU_GROUP_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `메뉴를 일괄 생성하면 모두 저장한다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        val menuGroup = MenuGroup.withId(1L, 1L, "메인", 0)
        val command = BulkCreateMenuCommand(
            shopId = 1L,
            menus = listOf(
                CreateMenuItem(menuGroupId = 1L, name = "짜장면", description = null, price = 8000L, displayOrder = 0),
                CreateMenuItem(menuGroupId = 1L, name = "짬뽕", description = null, price = 9000L, displayOrder = 1),
            ),
        )
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        every { menuGroupRepository.findAllById(listOf(1L)) } returns listOf(menuGroup)
        every { menuRepository.saveAll(any<List<Menu>>()) } answers { it.invocation.args[0] as List<Menu> }

        val actual = menuService.createMenus(command, owner)

        assertEquals(2, actual.size)
        assertEquals("짜장면", actual[0].name)
        assertEquals("짬뽕", actual[1].name)
    }

    @Test
    fun `빈 목록으로 메뉴를 일괄 생성하면 예외가 발생한다`() {
        val command = BulkCreateMenuCommand(shopId = 1L, menus = emptyList())

        val exception = assertThrows<BusinessException> { menuService.createMenus(command, owner) }

        assertEquals(ShopErrorCode.EMPTY_MENU_LIST, exception.errorCode)
    }

    @Test
    fun `다른 사람의 상점에 메뉴를 일괄 생성하면 예외가 발생한다`() {
        val shop = Shop.withId(1L, 999L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        val command = BulkCreateMenuCommand(
            shopId = 1L,
            menus = listOf(CreateMenuItem(menuGroupId = 1L, name = "짜장면", description = null, price = 8000L, displayOrder = 0)),
        )
        every { shopRepository.findById(1L) } returns Optional.of(shop)

        val exception = assertThrows<BusinessException> { menuService.createMenus(command, owner) }

        assertEquals(ShopErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 메뉴 그룹으로 일괄 생성하면 예외가 발생한다`() {
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        val command = BulkCreateMenuCommand(
            shopId = 1L,
            menus = listOf(CreateMenuItem(menuGroupId = 999L, name = "짜장면", description = null, price = 8000L, displayOrder = 0)),
        )
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        every { menuGroupRepository.findAllById(listOf(999L)) } returns emptyList()

        val exception = assertThrows<BusinessException> { menuService.createMenus(command, owner) }

        assertEquals(ShopErrorCode.MENU_GROUP_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 메뉴를 조회하면 예외가 발생한다`() {
        every { menuRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { menuService.getMenuById(999L) }

        assertEquals(ShopErrorCode.MENU_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `메뉴 정보를 수정하면 필드가 반영된다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        val command = UpdateMenuCommand(name = "짬뽕", description = "매콤한 짬뽕", price = 9000L, displayOrder = 1)

        val actual = menuService.update(1L, command, owner)

        assertEquals("짬뽕", actual.name)
        assertEquals(9000L, actual.price)
        assertEquals(1, actual.displayOrder)
    }

    @Test
    fun `다른 사람의 상점 메뉴를 수정하면 예외가 발생한다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        val shop = Shop.withId(1L, 999L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        val command = UpdateMenuCommand(name = "짬뽕", description = "매콤한 짬뽕", price = 9000L, displayOrder = 1)

        val exception = assertThrows<BusinessException> { menuService.update(1L, command, owner) }

        assertEquals(ShopErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `품절 처리하면 soldOut이 true가 된다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { shopRepository.findById(1L) } returns Optional.of(shop)

        menuService.markSoldOut(1L, owner)

        assertTrue(menu.soldOut)
    }

    @Test
    fun `품절 해제하면 soldOut이 false가 된다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        menu.soldOut = true
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { shopRepository.findById(1L) } returns Optional.of(shop)

        menuService.markInStock(1L, owner)

        assertFalse(menu.soldOut)
    }

    @Test
    fun `사장님이 자기 상점 메뉴에 이미지를 업로드하면 imageUrl이 저장된다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        every { menuImageStorage.store(any()) } returns "abc123.jpg"
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        val actual = menuService.uploadImage(1L, file, owner)

        assertEquals("abc123.jpg", actual.imageUrl)
    }

    @Test
    fun `다른 사람의 상점 메뉴에 이미지를 업로드하면 예외가 발생한다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        val shop = Shop.withId(1L, 999L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        val exception = assertThrows<BusinessException> { menuService.uploadImage(1L, file, owner) }

        assertEquals(ShopErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `사장님이 아닌 역할로 업로드하면 예외가 발생한다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        val shop = Shop.withId(1L, 1L, "가게", "서울", "0212345678", BigDecimal("37.5"), BigDecimal("127.0"))
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { shopRepository.findById(1L) } returns Optional.of(shop)
        val customer = AuthenticatedUser(userId = 1L, role = Role.CUSTOMER)
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        val exception = assertThrows<BusinessException> { menuService.uploadImage(1L, file, customer) }

        assertEquals(ShopErrorCode.NOT_SHOP_OWNER, exception.errorCode)
    }

    @Test
    fun `지원하지 않는 확장자로 업로드하면 예외가 발생한다`() {
        val file = MockMultipartFile("file", "malware.exe", "application/octet-stream", byteArrayOf(1, 2, 3))

        val exception = assertThrows<BusinessException> { menuService.uploadImage(1L, file, owner) }

        assertEquals(ShopErrorCode.INVALID_MENU_IMAGE, exception.errorCode)
    }

    @Test
    fun `빈 파일을 업로드하면 예외가 발생한다`() {
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", ByteArray(0))

        val exception = assertThrows<BusinessException> { menuService.uploadImage(1L, file, owner) }

        assertEquals(ShopErrorCode.INVALID_MENU_IMAGE, exception.errorCode)
    }

    @Test
    fun `확장자가 없는 파일을 업로드하면 예외가 발생한다`() {
        val file = MockMultipartFile("file", "noextension", "application/octet-stream", byteArrayOf(1, 2, 3))

        val exception = assertThrows<BusinessException> { menuService.uploadImage(1L, file, owner) }

        assertEquals(ShopErrorCode.INVALID_MENU_IMAGE, exception.errorCode)
    }

    @Test
    fun `이미지가 없는 메뉴의 경로를 조회하면 예외가 발생한다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        every { menuRepository.findById(1L) } returns Optional.of(menu)

        val exception = assertThrows<BusinessException> { menuService.getImagePath(1L) }

        assertEquals(ShopErrorCode.MENU_IMAGE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `저장소에 실제 파일이 없으면 예외가 발생한다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        menu.imageUrl = "missing.jpg"
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { menuImageStorage.exists("missing.jpg") } returns false

        val exception = assertThrows<BusinessException> { menuService.getImagePath(1L) }

        assertEquals(ShopErrorCode.MENU_IMAGE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `이미지가 있는 메뉴는 경로를 반환한다`() {
        val menu = Menu.withId(1L, 1L, 1L, "짜장면", 8000L, 0)
        menu.imageUrl = "exists.jpg"
        val expectedPath = Path.of("uploads/menu-images/exists.jpg")
        every { menuRepository.findById(1L) } returns Optional.of(menu)
        every { menuImageStorage.exists("exists.jpg") } returns true
        every { menuImageStorage.resolve("exists.jpg") } returns expectedPath

        val actual = menuService.getImagePath(1L)

        assertEquals(expectedPath, actual)
    }
}
