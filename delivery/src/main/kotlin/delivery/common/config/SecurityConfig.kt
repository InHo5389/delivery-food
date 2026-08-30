package delivery.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // ★ 게이트웨이 도입(커밋 10) 전까지 임시 설정.
    //   JWT 서명 검증 필터가 게이트웨이로 이동하면 이 필터체인은 auth-service 전용으로 축소된다.
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/auth/**", permitAll)
                authorize("/shops", permitAll)
                authorize("/shops/*", permitAll)
                authorize("/search/**", permitAll)
                // ★ 사장님 권한 체계(owner_profile)가 아직 없어 임시로 공개 처리.
                //   실제 권한 검증이 생기면 사장님 전용으로 좁혀야 한다.
                authorize("/menus/*/image", permitAll)
                authorize(anyRequest, authenticated)
            }
            httpBasic { disable() }
            formLogin { disable() }
        }
        return http.build()
    }
}
