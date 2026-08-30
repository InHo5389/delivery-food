package delivery.auth.api.dto

import delivery.auth.application.dto.SignupCommand
import delivery.auth.domain.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SignupRequest(
    @field:NotBlank @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val phone: String,
    val role: Role,
) {
    fun toCommand(): SignupCommand = SignupCommand(email, password, name, phone, role)
}
