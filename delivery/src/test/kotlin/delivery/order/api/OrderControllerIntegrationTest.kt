package delivery.order.api

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
import delivery.shop.domain.Menu
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class OrderControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val shopService: ShopService,
    @Autowired private val menuService: MenuService,
) : IntegrationTestSupport() {

    private data class SignedUpUser(val user: AuthenticatedUser, val token: String)
    private data class ShopWithMenu(val shopId: Long, val menu: Menu, val owner: SignedUpUser)

    private fun signup(email: String, role: Role): SignedUpUser {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "테스트유저", phone = "01011112222", role = role)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return SignedUpUser(AuthenticatedUser(userId, role), tokenPair.accessToken)
    }

    private fun setUpOpenShopWithMenu(minOrderAmount: Long = 0): ShopWithMenu {
        val owner = signup("order-owner-${System.nanoTime()}@test.com", Role.OWNER)
        val shop = shopService.create(
            CreateShopCommand(
                name = "가게",
                address = "서울",
                latitude = BigDecimal("37.5665000"),
                longitude = BigDecimal("126.9780000"),
                phone = "0212345678",
                minOrderAmount = minOrderAmount,
                deliveryFee = 0,
            ),
            owner.user,
        )
        shopService.open(shop.id!!, owner.user)
        val menuGroup = menuService.createMenuGroup(CreateMenuGroupCommand(shopId = shop.id!!, name = "메인", displayOrder = 0), owner.user)
        val menu = menuService.createMenu(
            CreateMenuCommand(shopId = shop.id!!, menuGroupId = menuGroup.id!!, name = "짜장면", description = null, price = 8000L, displayOrder = 0),
            owner.user,
        )
        return ShopWithMenu(shop.id!!, menu, owner)
    }

    private fun addToCart(token: String, shopId: Long, menu: Menu, quantity: Int = 1) {
        val body = """{"shopId":$shopId,"menuId":${menu.id},"menuName":"${menu.name}","menuPrice":${menu.price},"quantity":$quantity}"""
        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
    }

    @Test
    fun `장바구니를 담고 주문을 생성하면 201과 PAID 상태를 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val customer = signup("order-customer1@test.com", Role.CUSTOMER)
        addToCart(customer.token, shopWithMenu.shopId, shopWithMenu.menu)
        val body = """{"customerName":"홍길동","customerPhone":"01099998888"}"""

        mockMvc.perform(post("/orders").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.totalAmount").value(8000))
    }

    @Test
    fun `장바구니가 비어 있으면 400을 반환한다`() {
        val customer = signup("order-customer2@test.com", Role.CUSTOMER)
        val body = """{"customerName":"홍길동","customerPhone":"01099998888"}"""

        mockMvc.perform(post("/orders").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("EMPTY_CART"))
    }

    @Test
    fun `영업중이 아닌 상점의 메뉴로 주문하면 409를 반환한다`() {
        val owner = signup("order-owner-closed@test.com", Role.OWNER)
        val shop = shopService.create(
            CreateShopCommand(name = "닫은가게", address = "서울", latitude = BigDecimal("37.5665000"), longitude = BigDecimal("126.9780000"), phone = "0212345679", minOrderAmount = 0, deliveryFee = 0),
            owner.user,
        )
        val menuGroup = menuService.createMenuGroup(CreateMenuGroupCommand(shopId = shop.id!!, name = "메인", displayOrder = 0), owner.user)
        val menu = menuService.createMenu(CreateMenuCommand(shopId = shop.id!!, menuGroupId = menuGroup.id!!, name = "짜장면", description = null, price = 8000L, displayOrder = 0), owner.user)
        val customer = signup("order-customer3@test.com", Role.CUSTOMER)
        addToCart(customer.token, shop.id!!, menu)
        val body = """{"customerName":"홍길동","customerPhone":"01099998888"}"""

        mockMvc.perform(post("/orders").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("SHOP_NOT_OPEN"))
    }

    @Test
    fun `품절된 메뉴로 주문하면 409를 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val customer = signup("order-customer4@test.com", Role.CUSTOMER)
        addToCart(customer.token, shopWithMenu.shopId, shopWithMenu.menu)
        menuService.markSoldOut(shopWithMenu.menu.id!!, shopWithMenu.owner.user)
        val body = """{"customerName":"홍길동","customerPhone":"01099998888"}"""

        mockMvc.perform(post("/orders").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("MENU_SOLD_OUT"))
    }

    @Test
    fun `최소주문금액 미만이면 400을 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu(minOrderAmount = 20000L)
        val customer = signup("order-customer5@test.com", Role.CUSTOMER)
        addToCart(customer.token, shopWithMenu.shopId, shopWithMenu.menu)
        val body = """{"customerName":"홍길동","customerPhone":"01099998888"}"""

        mockMvc.perform(post("/orders").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BELOW_MIN_ORDER_AMOUNT"))
    }

    @Test
    fun `주문 생성 후 내 주문 목록에서 조회할 수 있다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val customer = signup("order-customer6@test.com", Role.CUSTOMER)
        addToCart(customer.token, shopWithMenu.shopId, shopWithMenu.menu)
        val body = """{"customerName":"홍길동","customerPhone":"01099998888"}"""
        mockMvc.perform(post("/orders").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)

        mockMvc.perform(get("/orders").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].menuName").value("짜장면"))
    }

    @Test
    fun `다른 사람의 주문을 조회하면 404를 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val customer = signup("order-customer7@test.com", Role.CUSTOMER)
        addToCart(customer.token, shopWithMenu.shopId, shopWithMenu.menu)
        val body = """{"customerName":"홍길동","customerPhone":"01099998888"}"""
        val response = mockMvc.perform(post("/orders").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andReturn()
        val orderId = Regex("\"orderIds\":\\[(\\d+)").find(response.response.contentAsString)!!.groupValues[1]

        val other = signup("order-customer8@test.com", Role.CUSTOMER)
        mockMvc.perform(get("/orders/$orderId").header("Authorization", "Bearer ${other.token}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `토큰 없이 주문을 생성하면 인증 오류를 반환한다`() {
        val body = """{"customerName":"홍길동","customerPhone":"01099998888"}"""

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is4xxClientError)
    }
}
