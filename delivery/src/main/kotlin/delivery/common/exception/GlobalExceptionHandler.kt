package delivery.common.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException, request: HttpServletRequest): ProblemDetail {
        log.warn("BusinessException: {}", e.message)
        return problemDetail(e.errorCode.status, e.errorCode.name, e.message ?: e.errorCode.message, request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException, request: HttpServletRequest): ProblemDetail {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return problemDetail(CommonErrorCode.INVALID_INPUT.status, CommonErrorCode.INVALID_INPUT.name, message, request)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException, request: HttpServletRequest): ProblemDetail {
        log.warn("HttpMessageNotReadableException: {}", e.message)
        return problemDetail(CommonErrorCode.INVALID_INPUT.status, CommonErrorCode.INVALID_INPUT.name, "요청 본문을 읽을 수 없습니다. 필드 누락 또는 형식을 확인해주세요.", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception, request: HttpServletRequest): ProblemDetail {
        log.error("Unhandled exception", e)
        return problemDetail(CommonErrorCode.INTERNAL_ERROR.status, CommonErrorCode.INTERNAL_ERROR.name, CommonErrorCode.INTERNAL_ERROR.message, request)
    }

    private fun problemDetail(status: HttpStatus, code: String, detail: String, request: HttpServletRequest): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            title = humanize(code)
            instance = URI.create(request.requestURI)
            setProperty("code", code)
        }

    private fun humanize(code: String): String =
        code.split('_').joinToString(" ") {
            it.lowercase().replaceFirstChar { c -> c.titlecase() }
        }
}
