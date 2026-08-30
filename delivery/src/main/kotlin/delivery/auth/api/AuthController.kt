package delivery.auth.api

import delivery.auth.api.dto.LoginRequest
import delivery.auth.api.dto.RefreshRequest
import delivery.auth.api.dto.SignupRequest
import delivery.auth.api.dto.TokenResponse
import delivery.auth.application.AuthService
import delivery.auth.application.TokenService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
) {
    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): TokenResponse =
        TokenResponse.from(authService.signup(request.toCommand()))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse =
        TokenResponse.from(authService.login(request.toCommand()))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): TokenResponse =
        TokenResponse.from(tokenService.refresh(request.refreshToken))
}
