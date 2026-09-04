package delivery.auth.application

import delivery.auth.application.dto.LoginCommand
import delivery.auth.application.dto.SignupCommand
import delivery.auth.application.dto.TokenPair
import delivery.auth.domain.Account
import delivery.auth.domain.AuthErrorCode
import delivery.auth.domain.Role
import delivery.auth.infrastructure.AccountRepository
import delivery.common.exception.BusinessException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val accountRepository: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
) {
    @Transactional
    fun signup(command: SignupCommand): TokenPair {
        if (command.role == Role.ADMIN) {
            throw BusinessException(AuthErrorCode.ADMIN_SIGNUP_NOT_ALLOWED)
        }
        if (accountRepository.existsByEmail(command.email)) {
            throw BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS)
        }

        val account = accountRepository.save(
            Account(
                email = command.email,
                password = passwordEncoder.encode(command.password)!!,
                name = command.name,
                phone = command.phone,
                role = command.role,
            )
        )

        return tokenService.issueTokenPair(account.id!!, account.role)
    }

    @Transactional
    fun login(command: LoginCommand): TokenPair {
        val account = accountRepository.findByEmail(command.email)
            ?: throw BusinessException(AuthErrorCode.INVALID_CREDENTIALS)

        if (!passwordEncoder.matches(command.password, account.password)) {
            throw BusinessException(AuthErrorCode.INVALID_CREDENTIALS)
        }

        return tokenService.issueTokenPair(account.id!!, account.role)
    }
}
