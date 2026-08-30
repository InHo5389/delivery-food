package delivery.auth.api.dto

import delivery.auth.application.dto.LoginCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
) {
    fun toCommand(): LoginCommand = LoginCommand(email, password)
}
