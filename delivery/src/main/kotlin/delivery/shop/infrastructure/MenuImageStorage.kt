package delivery.shop.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

// ⚠️ 의도적 구식 구현 — Phase 3 A-9에서 CDN + WebP 변환으로 개선 예정.
//   앱 서버(이 서버)가 로컬 디스크에 원본 이미지를 저장하고 직접 서빙한다.
@Component
class MenuImageStorage(
    @Value("\${menu.image.storage-dir:uploads/menu-images}") storageDir: String,
) {
    private val root: Path = Path.of(storageDir).toAbsolutePath().normalize().also {
        Files.createDirectories(it)
    }

    fun store(file: MultipartFile): String {
        val extension = file.originalFilename?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val filename = UUID.randomUUID().toString() + if (extension.isNotBlank()) ".$extension" else ""
        val target = root.resolve(filename)
        file.inputStream.use { Files.copy(it, target) }
        return filename
    }

    fun resolve(filename: String): Path {
        val resolved = root.resolve(filename).normalize()
        require(resolved.startsWith(root)) { "잘못된 파일 경로입니다." }
        return resolved
    }

    fun exists(filename: String): Boolean = Files.exists(resolve(filename))
}
