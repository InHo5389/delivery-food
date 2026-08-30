package delivery.common.exception

import org.springframework.http.HttpStatus

interface ErrorCode {
    val name: String
    val status: HttpStatus
    val message: String
}
