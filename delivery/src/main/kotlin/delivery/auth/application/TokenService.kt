package delivery.auth.application

import delivery.auth.application.dto.TokenPair
import delivery.auth.domain.AuthErrorCode
import delivery.auth.domain.RefreshToken
import delivery.auth.domain.Role
import delivery.auth.infrastructure.AccountRepository
import delivery.auth.infrastructure.JwtProvider
import delivery.auth.infrastructure.RefreshTokenRepository
import delivery.common.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Service
class TokenService(
    private val jwtProvider: JwtProvider,
    private val accountRepository: AccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    private val refreshTokenTtl: Duration = Duration.ofDays(14)
    private val secureRandom = SecureRandom()

    @Transactional
    fun issueTokenPair(accountId: Long, role: Role): TokenPair {
        val accessToken = jwtProvider.issueAccessToken(accountId, role)
        val refreshToken = generateOpaqueToken()
        refreshTokenRepository.save(
            RefreshToken(
                accountId = accountId,
                token = refreshToken,
                expiresAt = Instant.now().plus(refreshTokenTtl),
            )
        )
        return TokenPair(accessToken, refreshToken)
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        val saved = refreshTokenRepository.findByToken(refreshToken)
            ?: throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)

        if (saved.isExpired()) {
            throw BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        val account = accountRepository.findById(saved.accountId).orElseThrow {
            BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)
        }

        refreshTokenRepository.delete(saved)
        return issueTokenPair(account.id!!, account.role)
    }

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
