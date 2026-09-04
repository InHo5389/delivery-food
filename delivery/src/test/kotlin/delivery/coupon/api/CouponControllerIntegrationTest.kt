package delivery.coupon.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Account
import delivery.auth.domain.Role
import delivery.auth.infrastructure.AccountRepository
import delivery.auth.infrastructure.JwtProvider
import delivery.common.security.AuthenticatedUser
import delivery.coupon.application.CouponService
import delivery.coupon.domain.Coupon
import delivery.coupon.infrastructure.CouponRepository
import delivery.coupon.infrastructure.IssuanceRepository
import delivery.shop.application.ShopService
import delivery.shop.application.dto.CreateShopCommand
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
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class CouponControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val shopService: ShopService,
    @Autowired private val couponService: CouponService,
    @Autowired private val couponRepository: CouponRepository,
    @Autowired private val issuanceRepository: IssuanceRepository,
) : IntegrationTestSupport() {

    private data class SignedUp(val user: AuthenticatedUser, val token: String)

    private fun signup(email: String, role: Role): SignedUp {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "테스트", phone = "01011112222", role = role)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return SignedUp(AuthenticatedUser(userId, role), tokenPair.accessToken)
    }

    private fun issueAdminToken(email: String): SignedUp {
        val account = accountRepository.save(Account(email = email, password = "x", name = "운영자", phone = "01000000000", role = Role.ADMIN))
        val token = jwtProvider.issueAccessToken(account.id!!, Role.ADMIN)
        return SignedUp(AuthenticatedUser(account.id!!, Role.ADMIN), token)
    }

    private fun createShopId(owner: AuthenticatedUser): Long =
        shopService.create(
            CreateShopCommand(
                name = "가게", address = "서울",
                latitude = BigDecimal("37.5665000"), longitude = BigDecimal("126.9780000"),
                phone = "0212345678", minOrderAmount = 0, deliveryFee = 0,
            ),
            owner,
        ).id!!

    private fun newCoupon(totalQuantity: Int = 100, issuedQuantity: Int = 0, validityDays: Int = 7, startsAt: Instant? = null): Coupon =
        couponRepository.save(Coupon(name = "치킨 할인 쿠폰", totalQuantity = totalQuantity, issuedQuantity = issuedQuantity, validityDays = validityDays, startsAt = startsAt))

    @Test
    fun `ADMIN이 shopId 없이 쿠폰을 생성하면 201을 반환한다`() {
        val admin = issueAdminToken("coupon-admin1@test.com")
        val body = """{"name":"치킨 할인 쿠폰","totalQuantity":100,"validityDays":7}"""

        mockMvc.perform(post("/coupons").header("Authorization", "Bearer ${admin.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.shopId").doesNotExist())
            .andExpect(jsonPath("$.totalQuantity").value(100))
    }

    @Test
    fun `OWNER가 본인 상점의 쿠폰을 생성하면 201을 반환한다`() {
        val owner = signup("coupon-owner1@test.com", Role.OWNER)
        val shopId = createShopId(owner.user)
        val body = """{"name":"치킨 할인 쿠폰","shopId":$shopId,"totalQuantity":100,"validityDays":7}"""

        mockMvc.perform(post("/coupons").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.shopId").value(shopId))
    }

    @Test
    fun `OWNER가 shopId 없이 쿠폰을 생성하면 400을 반환한다`() {
        val owner = signup("coupon-owner2@test.com", Role.OWNER)
        val body = """{"name":"치킨 할인 쿠폰","totalQuantity":100,"validityDays":7}"""

        mockMvc.perform(post("/coupons").header("Authorization", "Bearer ${owner.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `OWNER가 다른 사람 상점의 쿠폰을 생성하면 403을 반환한다`() {
        val owner = signup("coupon-owner3@test.com", Role.OWNER)
        val shopId = createShopId(owner.user)
        val other = signup("coupon-owner4@test.com", Role.OWNER)
        val body = """{"name":"치킨 할인 쿠폰","shopId":$shopId,"totalQuantity":100,"validityDays":7}"""

        mockMvc.perform(post("/coupons").header("Authorization", "Bearer ${other.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `CUSTOMER는 쿠폰을 생성할 수 없다`() {
        val customer = signup("coupon-customer1@test.com", Role.CUSTOMER)
        val body = """{"name":"치킨 할인 쿠폰","totalQuantity":100,"validityDays":7}"""

        mockMvc.perform(post("/coupons").header("Authorization", "Bearer ${customer.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `수량이 0인 쿠폰을 생성하면 400을 반환한다`() {
        val admin = issueAdminToken("coupon-admin2@test.com")
        val body = """{"name":"치킨 할인 쿠폰","totalQuantity":0,"validityDays":7}"""

        mockMvc.perform(post("/coupons").header("Authorization", "Bearer ${admin.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `유효기간이 0일인 쿠폰을 생성하면 400을 반환한다`() {
        val admin = issueAdminToken("coupon-admin3@test.com")
        val body = """{"name":"치킨 할인 쿠폰","totalQuantity":100,"validityDays":0}"""

        mockMvc.perform(post("/coupons").header("Authorization", "Bearer ${admin.token}").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `쿠폰을 발급받으면 201을 반환한다`() {
        val customer = signup("coupon-issue1@test.com", Role.CUSTOMER)
        val coupon = newCoupon()

        mockMvc.perform(post("/coupons/${coupon.id}/issue").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.couponId").value(coupon.id))
            .andExpect(jsonPath("$.status").value("ISSUED"))
    }

    @Test
    fun `존재하지 않는 쿠폰을 발급받으려 하면 404를 반환한다`() {
        val customer = signup("coupon-issue2@test.com", Role.CUSTOMER)

        mockMvc.perform(post("/coupons/999999/issue").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `시작 전인 쿠폰을 발급받으려 하면 409를 반환한다`() {
        val customer = signup("coupon-issue3@test.com", Role.CUSTOMER)
        val coupon = newCoupon(startsAt = Instant.now().plusSeconds(3600))

        mockMvc.perform(post("/coupons/${coupon.id}/issue").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("NOT_STARTED"))
    }

    @Test
    fun `매진된 쿠폰을 발급받으려 하면 409를 반환한다`() {
        val customer = signup("coupon-issue4@test.com", Role.CUSTOMER)
        val coupon = newCoupon(totalQuantity = 1, issuedQuantity = 1)

        mockMvc.perform(post("/coupons/${coupon.id}/issue").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("SOLD_OUT"))
    }

    @Test
    fun `같은 쿠폰을 두 번 발급받으려 하면 409를 반환한다`() {
        val customer = signup("coupon-issue5@test.com", Role.CUSTOMER)
        val coupon = newCoupon()
        mockMvc.perform(post("/coupons/${coupon.id}/issue").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isCreated)

        mockMvc.perform(post("/coupons/${coupon.id}/issue").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("ALREADY_ISSUED"))
    }

    @Test
    fun `내 발급 목록을 조회하면 발급받은 쿠폰이 반환된다`() {
        val customer = signup("coupon-my1@test.com", Role.CUSTOMER)
        val coupon = newCoupon()
        mockMvc.perform(post("/coupons/${coupon.id}/issue").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isCreated)

        mockMvc.perform(get("/users/me/issuances").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.issuances.length()").value(1))
            .andExpect(jsonPath("$.issuances[0].couponId").value(coupon.id))
    }

    @Test
    fun `발급받은 쿠폰이 없으면 빈 목록을 반환한다`() {
        val customer = signup("coupon-my2@test.com", Role.CUSTOMER)

        mockMvc.perform(get("/users/me/issuances").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.issuances.length()").value(0))
    }

    // 커밋 53-9 PAAR의 동시성 검증 시나리오 — DB 비관적 락(FOR UPDATE)이 재고를 초과해
    // 발급하지 않고, 발급수량과 실제 발급 건수가 정확히 일치하는지 확인한다.
    // 문서의 측정 조건(재고 5,000 / 요청 6,000)은 CI에서 매번 돌리기엔 과하므로 스레드
    // 풀 규모만 줄였다 — 검증하려는 성질(초과 발급 없음, 카운터-실제 건수 일치)은 동일하다.
    @Test
    fun `재고보다 많은 동시 요청이 와도 정확히 재고만큼만 발급된다`() {
        val totalQuantity = 20
        val coupon = newCoupon(totalQuantity = totalQuantity)
        val concurrentRequests = 100
        val executor = Executors.newFixedThreadPool(20)
        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)

        repeat(concurrentRequests) { i ->
            executor.submit {
                try {
                    couponService.issue(coupon.id!!, userId = i.toLong())
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    // SOLD_OUT, 정상적인 경합 결과
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        val persisted = couponRepository.findById(coupon.id!!).get()
        assertEquals(totalQuantity, successCount.get())
        assertEquals(totalQuantity, persisted.issuedQuantity)
        assertEquals(totalQuantity, issuanceRepository.findAllByCouponId(coupon.id!!).size)
    }
}
