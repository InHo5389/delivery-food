package delivery.auth.infrastructure

import delivery.auth.domain.Role
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    private val accessTokenTtl: Duration = Duration.ofMinutes(15)

    fun issueAccessToken(accountId: Long, role: Role): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(accountId.toString())
            .claim("role", role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessTokenTtl)))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun isValid(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (e: JwtException) {
            false
        }
}
