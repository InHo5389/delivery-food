package delivery.common.security

import delivery.auth.domain.Role
import delivery.auth.infrastructure.JwtProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

// ★ 게이트웨이 도입(커밋 10) 전까지 임시로 이 서버가 직접 JWT를 검증한다.
//   Phase 5에서 게이트웨이가 서명 검증 + X-User-Id/X-Role 헤더 주입을 넘겨받으면
//   이 필터는 그 헤더를 읽어 AuthenticatedUser를 구성하는 방식으로 축소된다.
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)
        if (token != null && jwtProvider.isValid(token)) {
            runCatching {
                val claims = jwtProvider.parseClaims(token)
                val userId = claims.subject.toLong()
                val role = Role.valueOf(claims["role"] as String)
                AuthenticatedUser(userId, role)
            }.onSuccess { authenticatedUser ->
                val authentication = UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_${authenticatedUser.role.name}")),
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.removePrefix("Bearer ")
    }
}
