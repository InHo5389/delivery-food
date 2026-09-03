package delivery.delivery.domain

import java.math.BigDecimal
import java.math.RoundingMode

// 배차 후보 채점기.
// 가중치: 거리 40% / 최근 처리 건수 30% / 수락률 20% / 대기시간 10%.
//
// 네 요소의 단위(미터, 건, 비율, 초)가 전부 달라 절대값을 그대로 더할 수 없다.
// 그래서 이번 배차 사이클의 "후보군 안에서" 최소~최대 구간으로 정규화(min-max)한 뒤
// 가중합한다 — 절대적인 "좋은 거리"를 정의하는 대신, 지금 이 배달을 두고 경쟁하는
// 라이더끼리 상대적으로 비교한다.
object DispatchScorer {
    private const val DISTANCE_WEIGHT = 0.4
    private const val RECENT_COUNT_WEIGHT = 0.3
    private const val ACCEPTANCE_RATE_WEIGHT = 0.2
    private const val WAIT_TIME_WEIGHT = 0.1

    // 후보 riderId -> 점수(0.0000~1.0000). 입력이 비어 있으면 빈 맵을 반환한다.
    fun score(candidates: List<DispatchCandidate>): Map<Long, BigDecimal> {
        if (candidates.isEmpty()) return emptyMap()

        val distanceScores = normalize(candidates.map { it.distanceMeters }, higherIsBetter = false)
        val recentCountScores = normalize(candidates.map { it.recentDeliveryCount.toDouble() }, higherIsBetter = false)
        val acceptanceScores = normalize(candidates.map { it.acceptanceRate }, higherIsBetter = true)
        val waitScores = normalize(candidates.map { it.waitSeconds.toDouble() }, higherIsBetter = true)

        return candidates.indices.associate { i ->
            val weighted = distanceScores[i] * DISTANCE_WEIGHT +
                recentCountScores[i] * RECENT_COUNT_WEIGHT +
                acceptanceScores[i] * ACCEPTANCE_RATE_WEIGHT +
                waitScores[i] * WAIT_TIME_WEIGHT
            candidates[i].riderId to BigDecimal.valueOf(weighted).setScale(4, RoundingMode.HALF_UP)
        }
    }

    // min-max 정규화. 후보가 하나뿐이거나 값이 전부 같으면 우열을 가릴 기준이 없으므로
    // 전부 만점(1.0)을 준다 — 0.0을 주면 다른 요소의 가중치만으로 순위가 갈려 왜곡된다.
    private fun normalize(values: List<Double>, higherIsBetter: Boolean): List<Double> {
        val min = values.min()
        val max = values.max()
        if (max == min) return values.map { 1.0 }
        return values.map { v ->
            val ratio = (v - min) / (max - min)
            if (higherIsBetter) ratio else 1.0 - ratio
        }
    }
}
