package delivery.auth.api.dto

import delivery.auth.application.dto.TokenPair

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        fun from(tokenPair: TokenPair): TokenResponse =
            TokenResponse(tokenPair.accessToken, tokenPair.refreshToken)
    }
}
