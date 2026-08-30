package delivery.common.security

import delivery.auth.domain.Role

// ★ 지금은 모놀리스라 이 필터가 JWT를 직접 검증하지만,
//   Phase 5(MSA 분리)에서 게이트웨이가 이 역할을 넘겨받으면
//   각 서비스는 X-User-Id/X-Role 헤더만 읽어 이 타입으로 재구성하면 된다.
data class AuthenticatedUser(
    val userId: Long,
    val role: Role,
)
