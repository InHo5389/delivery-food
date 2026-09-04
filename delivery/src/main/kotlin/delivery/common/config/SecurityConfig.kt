package delivery.common.config

import delivery.auth.infrastructure.JwtProvider
import delivery.common.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // ★ 게이트웨이 도입(커밋 10) 전까지 임시 설정.
    //   JWT 서명 검증 필터가 게이트웨이로 이동하면 이 필터체인은 auth-service 전용으로 축소된다.
    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtProvider: JwtProvider): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/auth/**", permitAll)
                authorize(HttpMethod.GET, "/shops", permitAll)
                authorize(HttpMethod.GET, "/shops/*", permitAll)
                authorize("/search/**", permitAll)
                authorize(HttpMethod.GET, "/menus/*/image", permitAll)
                authorize("/owner-profile", hasRole("OWNER"))
                authorize("/owner-profile/**", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/shops", hasRole("OWNER"))
                authorize(HttpMethod.PUT, "/shops/*", hasRole("OWNER"))
                authorize(HttpMethod.DELETE, "/shops/*", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/shops/*/open", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/shops/*/close", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/shops/*/menu-groups", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/shops/*/menus", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/shops/*/menus/bulk", hasRole("OWNER"))
                authorize(HttpMethod.PUT, "/menus/*", hasRole("OWNER"))
                authorize(HttpMethod.DELETE, "/menus/*", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/menus/*/sold-out", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/menus/*/in-stock", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/menus/*/image", hasRole("OWNER"))
                authorize("/cart", hasRole("CUSTOMER"))
                authorize("/cart/**", hasRole("CUSTOMER"))
                authorize(HttpMethod.POST, "/orders", hasRole("CUSTOMER"))
                authorize(HttpMethod.GET, "/orders", hasRole("CUSTOMER"))
                // "/orders/*"(고객 전용, 아래)보다 먼저 와야 한다 — Security 규칙은 선언
                // 순서대로 첫 매치를 적용하는데, "/orders/*"가 "/orders/sales-summary"도
                // 매칭해버리기 때문이다.
                authorize(HttpMethod.GET, "/orders/sales-summary", hasRole("OWNER"))
                authorize(HttpMethod.GET, "/orders/*", hasRole("CUSTOMER"))
                authorize(HttpMethod.POST, "/orders/*/cancel", hasRole("CUSTOMER"))
                authorize(HttpMethod.POST, "/orders/*/accept", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/orders/*/reject", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/orders/*/cooking-start", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/orders/*/cooking-done", hasRole("OWNER"))
                authorize(HttpMethod.GET, "/order-tickets", hasRole("OWNER"))
                authorize(HttpMethod.POST, "/dispatch-offers/*/accept", hasRole("RIDER"))
                authorize(HttpMethod.GET, "/dispatch-queue", hasRole("RIDER"))
                authorize(HttpMethod.POST, "/dispatch-queue/claim", hasRole("RIDER"))
                authorize(HttpMethod.POST, "/deliveries/*/pickup", hasRole("RIDER"))
                authorize(HttpMethod.POST, "/deliveries/*/complete", hasRole("RIDER"))
                authorize(HttpMethod.GET, "/settlements/me", hasAnyRole("OWNER", "RIDER"))
                authorize(HttpMethod.GET, "/settlements/*/items", hasAnyRole("OWNER", "RIDER", "ADMIN"))
                authorize(HttpMethod.POST, "/coupons", hasAnyRole("ADMIN", "OWNER"))
                authorize("/admin/**", hasRole("ADMIN"))
                authorize(anyRequest, authenticated)
            }
            httpBasic { disable() }
            formLogin { disable() }
        }
        http.addFilterBefore(JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
