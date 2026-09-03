package delivery.delivery.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.Rider
import delivery.delivery.domain.RiderStatus
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.RiderRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import kotlin.test.assertEquals

class DispatchQueueControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val riderRepository: RiderRepository,
) : IntegrationTestSupport() {

    private data class SignedUpRider(val accountId: Long, val token: String)

    private fun riderToken(email: String): String =
        authService.signup(SignupCommand(email = email, password = "password1234", name = "라이더", phone = "01011112222", role = Role.RIDER)).accessToken

    private fun signupRider(email: String): SignedUpRider {
        val tokenPair = authService.signup(SignupCommand(email = email, password = "password1234", name = "라이더", phone = "01011112222", role = Role.RIDER))
        val accountId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return SignedUpRider(accountId, tokenPair.accessToken)
    }

    private fun offeringDelivery(): Delivery {
        val delivery = deliveryRepository.save(
            Delivery(orderId = System.nanoTime(), shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        return deliveryRepository.save(delivery)
    }

    @Test
    fun `배차 큐를 조회하면 OFFERING 상태의 배달이 포함된다`() {
        val token = riderToken("queue-rider1@test.com")
        val delivery = offeringDelivery()

        mockMvc.perform(get("/dispatch-queue").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[?(@.deliveryId == ${delivery.id})]").exists())
    }

    @Test
    fun `limit 파라미터로 조회 개수를 제한한다`() {
        val token = riderToken("queue-rider2@test.com")
        repeat(3) { offeringDelivery() }

        mockMvc.perform(get("/dispatch-queue?limit=2").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
    }

    @Test
    fun `limit이 1 미만이면 400을 반환한다`() {
        val token = riderToken("queue-rider3@test.com")

        mockMvc.perform(get("/dispatch-queue?limit=0").header("Authorization", "Bearer $token"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `limit이 100을 초과하면 400을 반환한다`() {
        val token = riderToken("queue-rider4@test.com")

        mockMvc.perform(get("/dispatch-queue?limit=101").header("Authorization", "Bearer $token"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `limit이 정확히 100이면 허용된다`() {
        val token = riderToken("queue-rider5@test.com")

        mockMvc.perform(get("/dispatch-queue?limit=100").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
    }

    @Test
    fun `라이더가 아닌 역할로 조회하면 403을 반환한다`() {
        val token = authService.signup(
            SignupCommand(email = "queue-customer1@test.com", password = "password1234", name = "고객", phone = "01011112222", role = Role.CUSTOMER)
        ).accessToken

        mockMvc.perform(get("/dispatch-queue").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `토큰 없이 조회하면 인증 오류를 반환한다`() {
        mockMvc.perform(get("/dispatch-queue"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `AVAILABLE 라이더가 클레임하면 큐에 있던 배달 하나를 배정받는다`() {
        val rider = signupRider("queue-claim-rider1@test.com")
        riderRepository.save(Rider(rider.accountId, BigDecimal("37.5665000"), BigDecimal("126.9780000"), status = RiderStatus.AVAILABLE))
        offeringDelivery()

        // 이 테스트 클래스 밖에서도 같은 MySQL 컨테이너에 OFFERING 배달을 남기는 통합
        // 테스트가 있어(공유 컨테이너), claimNext가 항상 방금 만든 delivery를 반환한다고
        // 단정할 수 없다 — 응답으로 받은 deliveryId를 그대로 검증 대상으로 삼는다.
        val response = mockMvc.perform(post("/dispatch-queue/claim").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isOk)
            .andReturn()
        val claimedDeliveryId = Regex("\"deliveryId\":(\\d+)").find(response.response.contentAsString)!!.groupValues[1].toLong()

        val persisted = deliveryRepository.findById(claimedDeliveryId).orElseThrow()
        assertEquals(DeliveryStatus.ASSIGNED, persisted.status)
        assertEquals(RiderStatus.BUSY, riderRepository.findByAccountId(rider.accountId)!!.status)
    }

    @Test
    fun `라이더 프로필이 없으면 클레임 시 404를 반환한다`() {
        val rider = signupRider("queue-claim-rider2@test.com")
        offeringDelivery()

        mockMvc.perform(post("/dispatch-queue/claim").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RIDER_NOT_FOUND"))
    }

    @Test
    fun `BUSY 상태의 라이더는 클레임할 수 없다`() {
        val rider = signupRider("queue-claim-rider3@test.com")
        riderRepository.save(Rider(rider.accountId, BigDecimal("37.5665000"), BigDecimal("126.9780000"), status = RiderStatus.BUSY))
        offeringDelivery()

        mockMvc.perform(post("/dispatch-queue/claim").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RIDER_NOT_AVAILABLE"))
    }

    @Test
    fun `라이더가 아닌 역할로 클레임을 시도하면 403을 반환한다`() {
        val token = authService.signup(
            SignupCommand(email = "queue-claim-customer1@test.com", password = "password1234", name = "고객", phone = "01011112222", role = Role.CUSTOMER)
        ).accessToken

        mockMvc.perform(post("/dispatch-queue/claim").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `토큰 없이 클레임을 시도하면 인증 오류를 반환한다`() {
        mockMvc.perform(post("/dispatch-queue/claim"))
            .andExpect(status().is4xxClientError)
    }
}
