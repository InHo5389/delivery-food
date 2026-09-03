package delivery.delivery.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import delivery.common.security.AuthenticatedUser
import delivery.delivery.domain.Delivery
import delivery.delivery.domain.DeliveryStatus
import delivery.delivery.domain.DispatchOffer
import delivery.delivery.domain.RiderStatus
import delivery.delivery.domain.Rider
import delivery.delivery.infrastructure.DeliveryRepository
import delivery.delivery.infrastructure.DispatchOfferRepository
import delivery.delivery.infrastructure.RiderRepository
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class DispatchOfferControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val deliveryRepository: DeliveryRepository,
    @Autowired private val riderRepository: RiderRepository,
    @Autowired private val dispatchOfferRepository: DispatchOfferRepository,
) : IntegrationTestSupport() {

    private data class SignedUpUser(val user: AuthenticatedUser, val token: String)

    private fun signup(email: String, role: Role): SignedUpUser {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "테스트유저", phone = "01011112222", role = role)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return SignedUpUser(AuthenticatedUser(userId, role), tokenPair.accessToken)
    }

    private fun newRider(accountId: Long): Rider =
        riderRepository.save(Rider(accountId, BigDecimal("37.5665000"), BigDecimal("126.9780000"), status = RiderStatus.AVAILABLE))

    private fun offeringDeliveryWithOffer(riderId: Long): DispatchOffer {
        val delivery = deliveryRepository.save(
            Delivery(orderId = System.nanoTime(), shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        deliveryRepository.save(delivery)
        return dispatchOfferRepository.save(DispatchOffer(deliveryId = delivery.id!!, riderId = riderId, score = BigDecimal("0.9000")))
    }

    @Test
    fun `본인에게 온 오퍼를 수락하면 200과 ACCEPTED 상태를 반환한다`() {
        val rider = signup("dispatch-rider1@test.com", Role.RIDER)
        val riderProfile = newRider(rider.user.userId)
        val offer = offeringDeliveryWithOffer(riderProfile.id!!)

        mockMvc.perform(post("/dispatch-offers/${offer.id}/accept").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
    }

    @Test
    fun `다른 라이더에게 온 오퍼를 수락하려 하면 403을 반환한다`() {
        val owner = signup("dispatch-rider2@test.com", Role.RIDER)
        newRider(owner.user.userId)
        val stranger = signup("dispatch-rider3@test.com", Role.RIDER)
        newRider(stranger.user.userId)
        val offer = offeringDeliveryWithOffer(riderRepository.findByAccountId(owner.user.userId)!!.id!!)

        mockMvc.perform(post("/dispatch-offers/${offer.id}/accept").header("Authorization", "Bearer ${stranger.token}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_YOUR_OFFER"))
    }

    @Test
    fun `존재하지 않는 오퍼를 수락하면 404를 반환한다`() {
        val rider = signup("dispatch-rider4@test.com", Role.RIDER)
        newRider(rider.user.userId)

        mockMvc.perform(post("/dispatch-offers/999999/accept").header("Authorization", "Bearer ${rider.token}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("OFFER_NOT_FOUND"))
    }

    @Test
    fun `이미 다른 라이더가 배정된 배달의 오퍼를 수락하면 409를 반환한다`() {
        val first = signup("dispatch-rider5@test.com", Role.RIDER)
        val firstProfile = newRider(first.user.userId)
        val second = signup("dispatch-rider6@test.com", Role.RIDER)
        val secondProfile = newRider(second.user.userId)

        val delivery = deliveryRepository.save(
            Delivery(orderId = System.nanoTime(), shopId = 1L, pickupLatitude = BigDecimal("37.5665000"), pickupLongitude = BigDecimal("126.9780000"))
        )
        delivery.transitionTo(DeliveryStatus.OFFERING)
        deliveryRepository.save(delivery)
        val firstOffer = dispatchOfferRepository.save(DispatchOffer(deliveryId = delivery.id!!, riderId = firstProfile.id!!, score = BigDecimal("0.9000")))
        val secondOffer = dispatchOfferRepository.save(DispatchOffer(deliveryId = delivery.id!!, riderId = secondProfile.id!!, score = BigDecimal("0.8000")))

        mockMvc.perform(post("/dispatch-offers/${firstOffer.id}/accept").header("Authorization", "Bearer ${first.token}"))
            .andExpect(status().isOk)

        mockMvc.perform(post("/dispatch-offers/${secondOffer.id}/accept").header("Authorization", "Bearer ${second.token}"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("DISPATCH_ALREADY_ASSIGNED"))
    }

    @Test
    fun `라이더가 아닌 역할로 수락을 시도하면 403을 반환한다`() {
        val customer = signup("dispatch-customer1@test.com", Role.CUSTOMER)

        mockMvc.perform(post("/dispatch-offers/1/accept").header("Authorization", "Bearer ${customer.token}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `토큰 없이 수락을 시도하면 인증 오류를 반환한다`() {
        mockMvc.perform(post("/dispatch-offers/1/accept"))
            .andExpect(status().is4xxClientError)
    }
}
