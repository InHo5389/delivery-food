package delivery.order.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import delivery.common.security.AuthenticatedUser
import delivery.order.domain.Order
import delivery.order.domain.OrderItem
import delivery.order.domain.OrderStatus
import delivery.order.infrastructure.OrderItemRepository
import delivery.order.infrastructure.OrderRepository
import delivery.shop.application.ShopService
import delivery.shop.application.dto.CreateShopCommand
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class OrderSalesSummaryControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val shopService: ShopService,
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val orderItemRepository: OrderItemRepository,
) : IntegrationTestSupport() {

    private data class SignedUpUser(val user: AuthenticatedUser, val token: String)

    private fun signup(email: String, role: Role): SignedUpUser {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "테스트유저", phone = "01011112222", role = role)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return SignedUpUser(AuthenticatedUser(userId, role), tokenPair.accessToken)
    }

    private fun newShop(owner: SignedUpUser): Long =
        shopService.create(
            CreateShopCommand(
                name = "가게", address = "서울",
                latitude = BigDecimal("37.5665000"), longitude = BigDecimal("126.9780000"),
                phone = "0212345678", minOrderAmount = 0, deliveryFee = 0,
            ),
            owner.user,
        ).id!!

    private fun deliveredOrder(shopId: Long, deliveredAt: Instant, menuPrice: Long, quantity: Int) {
        val order = orderRepository.save(Order(System.nanoTime(), shopId, "홍길동", "01011112222"))
        order.transitionTo(OrderStatus.PAID)
        order.transitionTo(OrderStatus.ACCEPTED)
        order.transitionTo(OrderStatus.RIDER_ASSIGNED)
        order.transitionTo(OrderStatus.PICKED_UP)
        order.transitionTo(OrderStatus.DELIVERED)
        order.updatedAt = deliveredAt
        orderRepository.save(order)
        orderItemRepository.save(OrderItem(orderId = order.id!!, menuId = 1L, menuName = "짜장면", menuPrice = menuPrice, quantity = quantity))
    }

    @Test
    fun `사장님이 자기 상점의 매출을 조회하면 DELIVERED 주문 합계를 반환한다`() {
        val owner = signup("sales-owner1@test.com", Role.OWNER)
        val shopId = newShop(owner)
        val date = LocalDate.of(2026, 9, 3)
        val noon = date.atTime(12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant()
        deliveredOrder(shopId, noon, menuPrice = 8000L, quantity = 2)

        mockMvc.perform(get("/orders/sales-summary?shopId=$shopId&date=$date").header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderCount").value(1))
            .andExpect(jsonPath("$.totalAmount").value(16000))
    }

    @Test
    fun `다른 사장님이 매출을 조회하면 403을 반환한다`() {
        val owner = signup("sales-owner2@test.com", Role.OWNER)
        val shopId = newShop(owner)
        val stranger = signup("sales-stranger1@test.com", Role.OWNER)

        mockMvc.perform(get("/orders/sales-summary?shopId=$shopId&date=2026-09-03").header("Authorization", "Bearer ${stranger.token}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_SHOP_OWNER"))
    }

    @Test
    fun `고객이 매출을 조회하면 403을 반환한다`() {
        val customer = signup("sales-customer1@test.com", Role.CUSTOMER)

        mockMvc.perform(get("/orders/sales-summary?shopId=1&date=2026-09-03").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `shopId가 없으면 400을 반환한다`() {
        val owner = signup("sales-owner3@test.com", Role.OWNER)

        mockMvc.perform(get("/orders/sales-summary?date=2026-09-03").header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `date가 없으면 400을 반환한다`() {
        val owner = signup("sales-owner4@test.com", Role.OWNER)

        mockMvc.perform(get("/orders/sales-summary?shopId=1").header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `해당 날짜에 배달완료 주문이 없으면 0을 반환한다`() {
        val owner = signup("sales-owner5@test.com", Role.OWNER)
        val shopId = newShop(owner)

        mockMvc.perform(get("/orders/sales-summary?shopId=$shopId&date=2026-01-01").header("Authorization", "Bearer ${owner.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.orderCount").value(0))
            .andExpect(jsonPath("$.totalAmount").value(0))
    }

    @Test
    fun `토큰 없이 조회하면 인증 오류를 반환한다`() {
        mockMvc.perform(get("/orders/sales-summary?shopId=1&date=2026-09-03"))
            .andExpect(status().is4xxClientError)
    }
}
