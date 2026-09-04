package delivery.notification.api

import delivery.auth.application.AuthService
import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import delivery.notification.application.NotificationService
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NotificationControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val authService: AuthService,
    @Autowired private val jwtProvider: JwtProvider,
    @Autowired private val notificationService: NotificationService,
) : IntegrationTestSupport() {

    private fun signup(email: String): Pair<Long, String> {
        val tokenPair = authService.signup(
            SignupCommand(email = email, password = "password1234", name = "테스트", phone = "01011112222", role = Role.CUSTOMER)
        )
        val userId = jwtProvider.parseClaims(tokenPair.accessToken).subject.toLong()
        return userId to tokenPair.accessToken
    }

    @Test
    fun `구독하면 비동기 SSE 연결이 시작된다`() {
        val (_, token) = signup("notification-sub1@test.com")

        mockMvc.perform(get("/notifications/subscribe").header("Authorization", "Bearer $token"))
            .andExpect(request().asyncStarted())
    }

    @Test
    fun `구독 없이 발행된 알림도 이력에는 남아 나중에 조회할 수 있다`() {
        val (userId, token) = signup("notification-hist1@test.com")
        notificationService.notify(userId, orderId = 100L, message = "주문이 접수되었습니다.")

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notifications.length()").value(1))
            .andExpect(jsonPath("$.notifications[0].orderId").value(100))
            .andExpect(jsonPath("$.notifications[0].message").value("주문이 접수되었습니다."))
    }

    @Test
    fun `알림이 없으면 빈 목록을 반환한다`() {
        val (_, token) = signup("notification-hist2@test.com")

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notifications.length()").value(0))
    }

    @Test
    fun `다른 사용자의 알림은 내 목록에 섞이지 않는다`() {
        val (userId1, token1) = signup("notification-hist3@test.com")
        val (userId2, _) = signup("notification-hist4@test.com")
        notificationService.notify(userId1, orderId = 100L, message = "내 알림")
        notificationService.notify(userId2, orderId = 200L, message = "남의 알림")

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer $token1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notifications.length()").value(1))
            .andExpect(jsonPath("$.notifications[0].message").value("내 알림"))
    }

    @Test
    fun `토큰 없이 구독하면 인증 오류를 반환한다`() {
        mockMvc.perform(get("/notifications/subscribe"))
            .andExpect(status().is4xxClientError)
    }
}
