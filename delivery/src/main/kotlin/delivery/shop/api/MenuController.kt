package delivery.shop.api

import delivery.shop.api.dto.MenuImageUploadResponse
import delivery.shop.application.MenuService
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

// ⚠️ 의도적 구식 구현 — Phase 3 A-9에서 CDN + WebP 변환으로 개선 예정.
//   이미지 업로드/서빙을 이 애플리케이션 서버가 직접 처리한다.
@RestController
@RequestMapping("/menus")
class MenuController(
    private val menuService: MenuService,
) {
    @PostMapping("/{menuId}/image")
    fun uploadImage(@PathVariable menuId: Long, @RequestParam file: MultipartFile): MenuImageUploadResponse {
        val menu = menuService.uploadImage(menuId, file)
        return MenuImageUploadResponse(menu.id!!, menu.imageUrl!!)
    }

    @GetMapping("/{menuId}/image")
    fun getImage(@PathVariable menuId: Long): ResponseEntity<FileSystemResource> {
        val path = menuService.getImagePath(menuId)
        val resource = FileSystemResource(path)
        val contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM)
        return ResponseEntity.ok().contentType(contentType).body(resource)
    }
}
