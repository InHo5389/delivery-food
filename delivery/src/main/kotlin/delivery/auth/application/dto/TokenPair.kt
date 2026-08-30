package delivery.auth.application.dto

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)
