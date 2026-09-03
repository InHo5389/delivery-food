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

class OrderAcceptanceControllerIntegrationTest(
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

    private fun setUpOpenShopWithMenu(): ShopWithMenu {
        val owner = signup("accept-owner-${System.nanoTime()}@test.com", Role.OWNER)
        val shop = shopService.create(
            CreateShopCommand(
                name = "가게", address = "서울",
                latitude = BigDecimal("37.5665000"), longitude = BigDecimal("126.9780000"),
                phone = "0212345678", minOrderAmount = 0, deliveryFee = 0,
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

    private fun createPaidOrder(shopWithMenu: ShopWithMenu): Long {
        val customer = signup("accept-customer-${System.nanoTime()}@test.com", Role.CUSTOMER)
        val body = """{"shopId":${shopWithMenu.shopId},"menuId":${shopWithMenu.menu.id},"menuName":"${shopWithMenu.menu.name}","menuPrice":${shopWithMenu.menu.price},"quantity":1}"""
        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
        val response = mockMvc.perform(
            post("/orders").header("Authorization", "Bearer ${customer.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"customerName":"홍길동","customerPhone":"01099998888"}""")
        ).andExpect(status().isCreated).andReturn()
        return Regex("\"orderId\":(\\d+)").find(response.response.contentAsString)!!.groupValues[1].toLong()
    }

    @Test
    fun `사장님이 PAID 주문을 수락하면 ACCEPTED가 된다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val orderId = createPaidOrder(shopWithMenu)

        mockMvc.perform(post("/orders/$orderId/accept").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
    }

    @Test
    fun `다른 상점 사장님이 수락하려 하면 403을 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val orderId = createPaidOrder(shopWithMenu)
        val stranger = signup("accept-stranger1@test.com", Role.OWNER)

        mockMvc.perform(post("/orders/$orderId/accept").header("Authorization", "Bearer ${stranger.token}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_SHOP_OWNER"))
    }

    @Test
    fun `고객이 주문 수락을 시도하면 403을 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val orderId = createPaidOrder(shopWithMenu)
        val customer = signup("accept-customer-role@test.com", Role.CUSTOMER)

        mockMvc.perform(post("/orders/$orderId/accept").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `이미 수락된 주문을 다시 수락하려 하면 409를 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val orderId = createPaidOrder(shopWithMenu)
        mockMvc.perform(post("/orders/$orderId/accept").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)

        mockMvc.perform(post("/orders/$orderId/accept").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"))
    }

    @Test
    fun `사장님이 주문을 거절하면 REJECTED가 된다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val orderId = createPaidOrder(shopWithMenu)

        mockMvc.perform(post("/orders/$orderId/reject").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REJECTED"))
    }

    @Test
    fun `수락 후 조리 시작과 조리 완료를 순서대로 호출하면 상태가 바뀐다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val orderId = createPaidOrder(shopWithMenu)
        mockMvc.perform(post("/orders/$orderId/accept").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)

        mockMvc.perform(post("/orders/$orderId/cooking-start").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COOKING"))

        mockMvc.perform(post("/orders/$orderId/cooking-done").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COOKED"))
    }

    @Test
    fun `수락 전에 조리 시작을 시도하면 409를 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val orderId = createPaidOrder(shopWithMenu)

        mockMvc.perform(post("/orders/$orderId/cooking-start").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"))
    }

    @Test
    fun `주문이 접수되면 사장님의 티켓 목록에서 조회된다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        createPaidOrder(shopWithMenu)

        mockMvc.perform(get("/order-tickets?shopId=${shopWithMenu.shopId}").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tickets.length()").value(1))
            .andExpect(jsonPath("$.tickets[0].status").value("PENDING"))
            .andExpect(jsonPath("$.tickets[0].items[0].menuName").value("짜장면"))
    }

    @Test
    fun `주문을 수락하면 티켓 상태도 함께 ACCEPTED로 바뀐다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val orderId = createPaidOrder(shopWithMenu)
        mockMvc.perform(post("/orders/$orderId/accept").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/order-tickets?shopId=${shopWithMenu.shopId}").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tickets[0].status").value("ACCEPTED"))
    }

    @Test
    fun `다른 사장님이 티켓 목록을 조회하면 403을 반환한다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        createPaidOrder(shopWithMenu)
        val stranger = signup("accept-stranger2@test.com", Role.OWNER)

        mockMvc.perform(get("/order-tickets?shopId=${shopWithMenu.shopId}").header("Authorization", "Bearer ${stranger.token}"))
            .andExpect(status().isForbidden)
    }
}
