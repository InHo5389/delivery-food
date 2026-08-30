package delivery.shop.api

import delivery.common.security.AuthenticatedUser
import delivery.shop.api.dto.CreateOwnerProfileRequest
import delivery.shop.api.dto.OwnerProfileResponse
import delivery.shop.api.dto.UpdateOwnerProfileRequest
import delivery.shop.application.OwnerProfileService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/owner-profile")
class OwnerProfileController(
    private val ownerProfileService: OwnerProfileService,
) {
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateOwnerProfileRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): OwnerProfileResponse =
        OwnerProfileResponse.from(ownerProfileService.create(request.toCommand(requester.userId)))

    @GetMapping
    fun getMyProfile(@AuthenticationPrincipal requester: AuthenticatedUser): OwnerProfileResponse =
        OwnerProfileResponse.from(ownerProfileService.getByAccountId(requester.userId))

    @PutMapping
    fun update(
        @Valid @RequestBody request: UpdateOwnerProfileRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): OwnerProfileResponse =
        OwnerProfileResponse.from(ownerProfileService.update(requester.userId, request.toCommand()))
}
