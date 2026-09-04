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

// 주문 상태 변경(커밋 54~63)이 실제로 알림 이력에 남는지 확인한다 — SSE 실시간 전송
// 자체는 NotificationControllerIntegrationTest/SseEmitterRegistryTest가 이미 검증하므로,
// 여기서는 "OrderService가 알림을 실제로 트리거하는가"만 확인한다.
class OrderNotificationIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val shopService: ShopService,
    @Autowired private val menuService: MenuService,
) : IntegrationTestSupport() {

    private data class SignedUpUser(val userId: Long, val token: String)
    private data class ShopWithMenu(val shopId: Long, val menu: Menu, val owner: SignedUpUser)

    private fun signup(email: String, role: Role): SignedUpUser {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "테스트", phone = "01011112222", role = role)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return SignedUpUser(userId, tokenPair.accessToken)
    }

    private fun setUpOpenShopWithMenu(): ShopWithMenu {
        val owner = signup("order-notif-owner-${System.nanoTime()}@test.com", Role.OWNER)
        val shop = shopService.create(
            CreateShopCommand(
                name = "가게", address = "서울",
                latitude = BigDecimal("37.5665000"), longitude = BigDecimal("126.9780000"),
                phone = "0212345678", minOrderAmount = 0, deliveryFee = 0,
            ),
            AuthenticatedUser(owner.userId, Role.OWNER),
        )
        shopService.open(shop.id!!, AuthenticatedUser(owner.userId, Role.OWNER))
        val menuGroup = menuService.createMenuGroup(
            CreateMenuGroupCommand(shopId = shop.id!!, name = "메인", displayOrder = 0),
            AuthenticatedUser(owner.userId, Role.OWNER),
        )
        val menu = menuService.createMenu(
            CreateMenuCommand(shopId = shop.id!!, menuGroupId = menuGroup.id!!, name = "짜장면", description = null, price = 8000L, displayOrder = 0),
            AuthenticatedUser(owner.userId, Role.OWNER),
        )
        return ShopWithMenu(shop.id!!, menu, owner)
    }

    private fun createPaidOrder(shopWithMenu: ShopWithMenu): Pair<Long, SignedUpUser> {
        val customer = signup("order-notif-customer-${System.nanoTime()}@test.com", Role.CUSTOMER)
        val body = """{"shopId":${shopWithMenu.shopId},"menuId":${shopWithMenu.menu.id},"menuName":"${shopWithMenu.menu.name}","menuPrice":${shopWithMenu.menu.price},"quantity":1}"""
        mockMvc.perform(post("/cart/items").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
        val response = mockMvc.perform(
            post("/orders").header("Authorization", "Bearer ${customer.token}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"customerName":"홍길동","customerPhone":"01099998888"}""")
        ).andExpect(status().isCreated).andReturn()
        val orderId = Regex("\"orderId\":(\\d+)").find(response.response.contentAsString)!!.groupValues[1].toLong()
        return orderId to customer
    }

    @Test
    fun `사장님이 주문을 수락하면 고객 알림 이력에 접수 메시지가 남는다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val (orderId, customer) = createPaidOrder(shopWithMenu)

        mockMvc.perform(
            post("/orders/$orderId/accept").header("Authorization", "Bearer ${shopWithMenu.owner.token}")
                .contentType(MediaType.APPLICATION_JSON).content("""{"estimatedCookingMinutes":15}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notifications[0].orderId").value(orderId))
            .andExpect(jsonPath("$.notifications[0].message").value("주문이 접수되었습니다."))
    }

    @Test
    fun `사장님이 주문을 거절하면 고객 알림 이력에 거절 메시지가 남는다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val (orderId, customer) = createPaidOrder(shopWithMenu)

        mockMvc.perform(post("/orders/$orderId/reject").header("Authorization", "Bearer ${shopWithMenu.owner.token}"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notifications[0].message").value("주문이 거절되었습니다."))
    }

    @Test
    fun `고객이 주문을 취소하면 알림 이력에 취소 메시지가 남는다`() {
        val shopWithMenu = setUpOpenShopWithMenu()
        val (orderId, customer) = createPaidOrder(shopWithMenu)

        mockMvc.perform(post("/orders/$orderId/cancel").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notifications[0].message").value("주문이 취소되었습니다."))
    }
}
