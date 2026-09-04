package delivery.settlement.api

import delivery.common.security.AuthenticatedUser
import delivery.settlement.api.dto.AdminSettlementListRequest
import delivery.settlement.api.dto.AdminSettlementListResponse
import delivery.settlement.api.dto.MySettlementRequest
import delivery.settlement.api.dto.SettlementItemListResponse
import delivery.settlement.api.dto.SettlementItemResponse
import delivery.settlement.api.dto.SettlementResponse
import delivery.settlement.application.SettlementQueryService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SettlementController(
    private val settlementQueryService: SettlementQueryService,
) {
    @GetMapping("/settlements/me")
    fun getMySettlement(
        @Valid @ModelAttribute request: MySettlementRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): SettlementResponse = SettlementResponse.from(settlementQueryService.getMySettlement(requester, request.toQuery()))

    @GetMapping("/settlements/{settlementId}/items")
    fun getSettlementItems(
        @PathVariable settlementId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): SettlementItemListResponse =
        SettlementItemListResponse(
            settlementQueryService.getSettlementItems(requester, settlementId).map(SettlementItemResponse::from)
        )

    @GetMapping("/admin/settlements")
    fun getAdminSettlements(
        @Valid @ModelAttribute request: AdminSettlementListRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): AdminSettlementListResponse =
        AdminSettlementListResponse(
            settlementQueryService.getAdminSettlements(requester, request.toQuery()).map(SettlementResponse::from)
        )

    @PostMapping("/admin/settlements/{settlementId}/confirm")
    fun confirmSettlement(
        @PathVariable settlementId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): SettlementResponse = SettlementResponse.from(settlementQueryService.confirmSettlement(requester, settlementId))

    @PostMapping("/admin/settlements/{settlementId}/pay")
    fun paySettlement(
        @PathVariable settlementId: Long,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): SettlementResponse = SettlementResponse.from(settlementQueryService.paySettlement(requester, settlementId))
}
