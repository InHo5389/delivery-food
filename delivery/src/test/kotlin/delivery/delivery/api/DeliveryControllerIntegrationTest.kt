package delivery.delivery.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import delivery.common.security.AuthenticatedUser
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class DeliveryControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val riderRepository: RiderRepository,
) : IntegrationTestSupport() {

    private data class SignedUpUser(val user: AuthenticatedUser, val token: String)

    private fun signup(email: String, role: Role): SignedUpUser {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "테스트유저", phone = "01011112222", role = role)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return SignedUpUser(AuthenticatedUser(userId, role), tokenPair.accessToken)
    }

    private fun newRider(accountId: Long, status: RiderStatus = RiderStatus.BUSY): Rider =
        riderRepository.save(Rider(accountId, BigDecimal("37.5665000"), BigDecimal("126.9780000"), status = status))

    private fun assignedDelivery(riderId: Long): Delivery {
        val delivery = deliveryRepository.save(
            Delivery(orderId = System.nanoTime(), shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        delivery.transitionTo(DeliveryStatus.ASSIGNED)
        delivery.riderId = riderId
        return deliveryRepository.save(delivery)
    }

    @Test
    fun `배정된 라이더가 픽업 처리하면 PICKED_UP으로 바뀐다`() {
        val rider = signup("delivery-rider1@test.com", Role.RIDER)
        val riderProfile = newRider(rider.user.userId)
        val delivery = assignedDelivery(riderProfile.id!!)

        mockMvc.perform(post("/deliveries/${delivery.id}/pickup").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PICKED_UP"))
    }

    @Test
    fun `픽업 후 완료 처리하면 DELIVERED로 바뀌고 라이더는 다시 배차를 받을 수 있다`() {
        val rider = signup("delivery-rider2@test.com", Role.RIDER)
        val riderProfile = newRider(rider.user.userId)
        val delivery = assignedDelivery(riderProfile.id!!)
        mockMvc.perform(post("/deliveries/${delivery.id}/pickup").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isOk)

        mockMvc.perform(post("/deliveries/${delivery.id}/complete").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DELIVERED"))

        val persistedRider = riderRepository.findByAccountId(rider.user.userId)!!
        kotlin.test.assertEquals(RiderStatus.AVAILABLE, persistedRider.status)
    }

    @Test
    fun `픽업 전에 완료 처리를 시도하면 409를 반환한다`() {
        val rider = signup("delivery-rider3@test.com", Role.RIDER)
        val riderProfile = newRider(rider.user.userId)
        val delivery = assignedDelivery(riderProfile.id!!)

        mockMvc.perform(post("/deliveries/${delivery.id}/complete").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVALID_DELIVERY_STATUS_TRANSITION"))
    }

    @Test
    fun `다른 라이더에게 배정된 배달을 픽업 처리하면 403을 반환한다`() {
        val owner = signup("delivery-rider4@test.com", Role.RIDER)
        val ownerProfile = newRider(owner.user.userId)
        val delivery = assignedDelivery(ownerProfile.id!!)
        val stranger = signup("delivery-rider5@test.com", Role.RIDER)
        newRider(stranger.user.userId)

        mockMvc.perform(post("/deliveries/${delivery.id}/pickup").header("Authorization", "Bearer ${stranger.token}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_YOUR_DELIVERY"))
    }

    @Test
    fun `존재하지 않는 배달을 픽업 처리하면 404를 반환한다`() {
        val rider = signup("delivery-rider6@test.com", Role.RIDER)
        newRider(rider.user.userId)

        mockMvc.perform(post("/deliveries/999999/pickup").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("DELIVERY_NOT_FOUND"))
    }

    @Test
    fun `라이더가 아닌 역할로 픽업을 시도하면 403을 반환한다`() {
        val customer = signup("delivery-customer1@test.com", Role.CUSTOMER)

        mockMvc.perform(post("/deliveries/1/pickup").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `토큰 없이 픽업을 시도하면 인증 오류를 반환한다`() {
        mockMvc.perform(post("/deliveries/1/pickup"))
            .andExpect(status().is4xxClientError)
    }
}
