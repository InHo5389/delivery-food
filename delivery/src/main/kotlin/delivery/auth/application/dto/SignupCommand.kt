package delivery.auth.application.dto

import delivery.auth.domain.Role

data class SignupCommand(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val role: Role,
)
