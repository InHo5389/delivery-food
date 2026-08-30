package delivery.auth.application.dto

data class LoginCommand(
    val email: String,
    val password: String,
)
