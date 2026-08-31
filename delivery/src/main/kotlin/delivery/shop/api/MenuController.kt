package delivery.shop.api

import delivery.common.security.AuthenticatedUser
import delivery.shop.api.dto.BulkCreateMenuRequest
import delivery.shop.api.dto.CreateMenuGroupRequest
import delivery.shop.api.dto.CreateMenuRequest
import delivery.shop.api.dto.MenuCreateResponse
import delivery.shop.api.dto.MenuGroupCreateResponse
import delivery.shop.api.dto.MenuImageUploadResponse
import delivery.shop.api.dto.UpdateMenuRequest
import delivery.shop.application.MenuService
import jakarta.validation.Valid
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class MenuController(
    private val menuService: MenuService,
) {
    @PostMapping("/shops/{shopId}/menu-groups")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMenuGroup(
        @PathVariable shopId: Long,
        @Valid @RequestBody request: CreateMenuGroupRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): MenuGroupCreateResponse =
        MenuGroupCreateResponse.from(menuService.createMenuGroup(request.toCommand(shopId), requester))

    @PostMapping("/shops/{shopId}/menus")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMenu(
        @PathVariable shopId: Long,
        @Valid @RequestBody request: CreateMenuRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): MenuCreateResponse = MenuCreateResponse.from(menuService.createMenu(request.toCommand(shopId), requester))

    @PostMapping("/shops/{shopId}/menus/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMenus(
        @PathVariable shopId: Long,
        @Valid @RequestBody request: BulkCreateMenuRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): List<MenuCreateResponse> =
        menuService.createMenus(request.toCommand(shopId), requester).map(MenuCreateResponse::from)

    @PutMapping("/menus/{menuId}")
    fun updateMenu(
        @PathVariable menuId: Long,
        @Valid @RequestBody request: UpdateMenuRequest,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): MenuCreateResponse = MenuCreateResponse.from(menuService.update(menuId, request.toCommand(), requester))

    @PostMapping("/menus/{menuId}/sold-out")
    fun markSoldOut(@PathVariable menuId: Long, @AuthenticationPrincipal requester: AuthenticatedUser) {
        menuService.markSoldOut(menuId, requester)
    }

    @PostMapping("/menus/{menuId}/in-stock")
    fun markInStock(@PathVariable menuId: Long, @AuthenticationPrincipal requester: AuthenticatedUser) {
        menuService.markInStock(menuId, requester)
    }

    @DeleteMapping("/menus/{menuId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMenu(@PathVariable menuId: Long, @AuthenticationPrincipal requester: AuthenticatedUser) {
        menuService.delete(menuId, requester)
    }

    // ⚠️ 의도적 구식 구현 — Phase 3 A-9에서 CDN + WebP 변환으로 개선 예정.
    //   이미지 업로드/서빙을 이 애플리케이션 서버가 직접 처리한다.
    @PostMapping("/menus/{menuId}/image")
    fun uploadImage(
        @PathVariable menuId: Long,
        @RequestParam file: MultipartFile,
        @AuthenticationPrincipal requester: AuthenticatedUser,
    ): MenuImageUploadResponse {
        val menu = menuService.uploadImage(menuId, file, requester)
        return MenuImageUploadResponse(menu.id!!, menu.imageUrl!!)
    }

    @GetMapping("/menus/{menuId}/image")
    fun getImage(@PathVariable menuId: Long): ResponseEntity<FileSystemResource> {
        val path = menuService.getImagePath(menuId)
        val resource = FileSystemResource(path)
        val contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM)
        return ResponseEntity.ok().contentType(contentType).body(resource)
    }
}
