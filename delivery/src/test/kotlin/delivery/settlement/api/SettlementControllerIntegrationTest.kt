package delivery.settlement.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Account
import delivery.auth.domain.Role
import delivery.auth.infrastructure.AccountRepository
import delivery.auth.infrastructure.JwtProvider
import delivery.settlement.domain.Settlement
import delivery.settlement.domain.SettlementItem
import delivery.settlement.domain.SettlementItemType
import delivery.settlement.domain.SettlementStatus
import delivery.settlement.domain.SettlementTargetType
import delivery.settlement.infrastructure.SettlementItemRepository
import delivery.settlement.infrastructure.SettlementRepository
import delivery.shop.domain.Shop
import delivery.shop.infrastructure.ShopRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZoneId

class SettlementControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val shopRepository: ShopRepository,
    @Autowired private val settlementRepository: SettlementRepository,
    @Autowired private val settlementItemRepository: SettlementItemRepository,
) : IntegrationTestSupport() {

    private val zone = ZoneId.of("Asia/Seoul")

    private fun issueToken(email: String, role: Role, name: String = "테스트"): Pair<Long, String> {
        val tokenPair = authService.signup(SignupCommand(email = email, password = "password1234", name = name, phone = "01011112222", role = role))
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return userId to tokenPair.accessToken
    }

    // 운영자 계정은 회원가입으로 만들 수 없어(53-4) DB에 직접 심는다.
    private fun issueAdminToken(email: String): String {
        val account = accountRepository.save(Account(email = email, password = "x", name = "운영자", phone = "01000000000", role = Role.ADMIN))
        return jwtProvider.issueAccessToken(account.id!!, Role.ADMIN)
    }

    private fun newShop(ownerId: Long): Shop =
        shopRepository.save(Shop(ownerId, "가게", "서울", BigDecimal("37.5665000"), BigDecimal("126.9780000"), "0212345678"))

    private fun monthRange(yearMonth: YearMonth) =
        yearMonth.atDay(1).atStartOfDay(zone).toInstant() to yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

    private fun newShopSettlement(shopId: Long, yearMonth: YearMonth, totalAmount: Long = 16_000L): Settlement {
        val (start, end) = monthRange(yearMonth)
        val settlement = settlementRepository.save(Settlement(SettlementTargetType.SHOP, shopId, start, end, totalAmount = totalAmount))
        settlementItemRepository.save(
            SettlementItem(settlement.id!!, orderId = 101L, SettlementItemType.SALE, amount = 20_000L, appliedFeeRate = BigDecimal("0.2000"), settlementAmount = 16_000L)
        )
        return settlement
    }

    @Test
    fun `사장님이 본인 상점의 정산을 조회하면 200과 결과를 반환한다`() {
        val (ownerId, token) = issueToken("owner-settlement1@test.com", Role.OWNER)
        val shop = newShop(ownerId)
        newShopSettlement(shop.id!!, YearMonth.of(2026, 3))

        mockMvc.perform(get("/settlements/me?yearMonth=2026-03&shopId=${shop.id}").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.grossAmount").value(20000))
            .andExpect(jsonPath("$.payoutAmount").value(16000))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `다른 사람의 상점 정산을 조회하면 403을 반환한다`() {
        val (ownerId, _) = issueToken("owner-settlement2@test.com", Role.OWNER)
        val shop = newShop(ownerId)
        newShopSettlement(shop.id!!, YearMonth.of(2026, 3))
        val (_, strangerToken) = issueToken("owner-settlement3@test.com", Role.OWNER)

        mockMvc.perform(get("/settlements/me?yearMonth=2026-03&shopId=${shop.id}").header("Authorization", "Bearer $strangerToken"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `shopId 없이 조회하면 400을 반환한다`() {
        val (_, token) = issueToken("owner-settlement4@test.com", Role.OWNER)

        mockMvc.perform(get("/settlements/me?yearMonth=2026-03").header("Authorization", "Bearer $token"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `해당 기간의 정산이 없으면 404를 반환한다`() {
        val (ownerId, token) = issueToken("owner-settlement5@test.com", Role.OWNER)
        val shop = newShop(ownerId)

        mockMvc.perform(get("/settlements/me?yearMonth=2026-05&shopId=${shop.id}").header("Authorization", "Bearer $token"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `잘못된 연월 형식으로 조회하면 400을 반환한다`() {
        val (ownerId, token) = issueToken("owner-settlement6@test.com", Role.OWNER)
        val shop = newShop(ownerId)

        mockMvc.perform(get("/settlements/me?yearMonth=2026년3월&shopId=${shop.id}").header("Authorization", "Bearer $token"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `고객 역할로 조회하면 403을 반환한다`() {
        val (_, token) = issueToken("customer-settlement1@test.com", Role.CUSTOMER)

        mockMvc.perform(get("/settlements/me?yearMonth=2026-03").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `토큰 없이 조회하면 인증 오류를 반환한다`() {
        mockMvc.perform(get("/settlements/me?yearMonth=2026-03"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `사장님이 본인 정산의 항목 목록을 조회한다`() {
        val (ownerId, token) = issueToken("owner-settlement7@test.com", Role.OWNER)
        val shop = newShop(ownerId)
        val settlement = newShopSettlement(shop.id!!, YearMonth.of(2026, 3))

        mockMvc.perform(get("/settlements/${settlement.id}/items").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].orderId").value(101))
            .andExpect(jsonPath("$.items[0].type").value("SALE"))
    }

    @Test
    fun `다른 사람의 정산 항목을 조회하면 403을 반환한다`() {
        val (ownerId, _) = issueToken("owner-settlement8@test.com", Role.OWNER)
        val shop = newShop(ownerId)
        val settlement = newShopSettlement(shop.id!!, YearMonth.of(2026, 3))
        val (_, strangerToken) = issueToken("owner-settlement9@test.com", Role.OWNER)

        mockMvc.perform(get("/settlements/${settlement.id}/items").header("Authorization", "Bearer $strangerToken"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `존재하지 않는 정산 항목을 조회하면 404를 반환한다`() {
        val (_, token) = issueToken("owner-settlement10@test.com", Role.OWNER)

        mockMvc.perform(get("/settlements/999999/items").header("Authorization", "Bearer $token"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `운영자가 아니면 전체 정산 목록을 조회할 수 없다`() {
        val (_, token) = issueToken("owner-settlement11@test.com", Role.OWNER)

        mockMvc.perform(get("/admin/settlements?from=2026-01&to=2026-03").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `운영자는 기간 범위의 전체 정산 목록을 조회할 수 있다`() {
        val (ownerId, _) = issueToken("owner-settlement12@test.com", Role.OWNER)
        val shop = newShop(ownerId)
        newShopSettlement(shop.id!!, YearMonth.of(2026, 2))
        val adminToken = issueAdminToken("admin-settlement1@test.com")

        mockMvc.perform(get("/admin/settlements?from=2026-01&to=2026-03").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
    }

    @Test
    fun `운영자가 정산을 확정하고 지급 완료 처리하면 상태가 순서대로 바뀐다`() {
        val (ownerId, _) = issueToken("owner-settlement13@test.com", Role.OWNER)
        val shop = newShop(ownerId)
        val settlement = newShopSettlement(shop.id!!, YearMonth.of(2026, 3))
        val adminToken = issueAdminToken("admin-settlement2@test.com")

        mockMvc.perform(post("/admin/settlements/${settlement.id}/confirm").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))

        mockMvc.perform(post("/admin/settlements/${settlement.id}/pay").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAID"))
    }

    @Test
    fun `운영자가 아니면 정산을 확정할 수 없다`() {
        val (ownerId, token) = issueToken("owner-settlement14@test.com", Role.OWNER)
        val shop = newShop(ownerId)
        val settlement = newShopSettlement(shop.id!!, YearMonth.of(2026, 3))

        mockMvc.perform(post("/admin/settlements/${settlement.id}/confirm").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PENDING 상태에서 바로 지급 완료를 시도하면 400을 반환한다`() {
        val (ownerId, _) = issueToken("owner-settlement15@test.com", Role.OWNER)
        val shop = newShop(ownerId)
        val settlement = newShopSettlement(shop.id!!, YearMonth.of(2026, 3))
        val adminToken = issueAdminToken("admin-settlement3@test.com")

        mockMvc.perform(post("/admin/settlements/${settlement.id}/pay").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `회원가입 시 ADMIN 역할은 400을 반환한다`() {
        val body = """
            {"email":"self-admin@test.com","password":"password1234","name":"관리자시도","phone":"01099998888","role":"ADMIN"}
        """.trimIndent()

        mockMvc.perform(
            post("/auth/signup")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest)
    }
}
