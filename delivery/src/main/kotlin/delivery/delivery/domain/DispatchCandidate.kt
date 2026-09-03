package delivery.delivery.domain

// 배차 채점 대상 라이더의 스냅샷. Rider 엔티티가 아니라 매칭 시점에 필요한 지표만 담은
// 값 객체다 — DispatchScorer가 순수 함수로 남아 Spring 컨텍스트 없이 단위 테스트 가능하게 한다.
data class DispatchCandidate(
    val riderId: Long,
    val distanceMeters: Double,
    val recentDeliveryCount: Int,
    val acceptanceRate: Double,
    val waitSeconds: Long,
)
