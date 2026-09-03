package delivery.delivery.api

import delivery.common.security.AuthenticatedUser
import delivery.delivery.api.dto.DispatchQueueItemResponse
import delivery.delivery.api.dto.DispatchQueueRequest
import delivery.delivery.api.dto.DispatchQueueResponse
import delivery.delivery.application.DispatchQueueService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dispatch-queue")
class DispatchQueueController(
    private val dispatchQueueService: DispatchQueueService,
) {
    @GetMapping
    fun getQueue(@Valid @ModelAttribute request: DispatchQueueRequest): DispatchQueueResponse =
        DispatchQueueResponse.from(dispatchQueueService.getQueue(request.limit))

    @PostMapping("/claim")
    fun claim(@AuthenticationPrincipal requester: AuthenticatedUser): DispatchQueueItemResponse =
        DispatchQueueItemResponse.from(dispatchQueueService.claim(requester.userId))
}
