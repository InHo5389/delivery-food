package delivery.delivery.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatchScorerTest {

    private fun candidate(
        riderId: Long,
        distanceMeters: Double = 1000.0,
        recentDeliveryCount: Int = 0,
        acceptanceRate: Double = 1.0,
        waitSeconds: Long = 0,
    ) = DispatchCandidate(riderId, distanceMeters, recentDeliveryCount, acceptanceRate, waitSeconds)

    @Test
    fun `후보가 없으면 빈 맵을 반환한다`() {
        val actual = DispatchScorer.score(emptyList())

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `후보가 하나뿐이면 만점을 받는다`() {
        val actual = DispatchScorer.score(listOf(candidate(riderId = 1L)))

        assertEquals(0, actual.getValue(1L).compareTo(1.0.toBigDecimal()))
    }

    @Test
    fun `모든 지표가 동일한 후보끼리는 동점이다`() {
        val candidates = listOf(
            candidate(riderId = 1L, distanceMeters = 500.0, recentDeliveryCount = 2, acceptanceRate = 0.8, waitSeconds = 30),
            candidate(riderId = 2L, distanceMeters = 500.0, recentDeliveryCount = 2, acceptanceRate = 0.8, waitSeconds = 30),
        )

        val actual = DispatchScorer.score(candidates)

        assertEquals(actual.getValue(1L), actual.getValue(2L))
    }

    @Test
    fun `다른 조건이 같으면 더 가까운 후보가 더 높은 점수를 받는다`() {
        val near = candidate(riderId = 1L, distanceMeters = 300.0)
        val far = candidate(riderId = 2L, distanceMeters = 2900.0)

        val actual = DispatchScorer.score(listOf(near, far))

        assertTrue(actual.getValue(1L) > actual.getValue(2L))
    }

    @Test
    fun `다른 조건이 같으면 최근 처리 건수가 적은 후보가 더 높은 점수를 받는다`() {
        val lessBusy = candidate(riderId = 1L, recentDeliveryCount = 1)
        val moreBusy = candidate(riderId = 2L, recentDeliveryCount = 9)

        val actual = DispatchScorer.score(listOf(lessBusy, moreBusy))

        assertTrue(actual.getValue(1L) > actual.getValue(2L))
    }

    @Test
    fun `다른 조건이 같으면 수락률이 높은 후보가 더 높은 점수를 받는다`() {
        val reliable = candidate(riderId = 1L, acceptanceRate = 0.95)
        val unreliable = candidate(riderId = 2L, acceptanceRate = 0.2)

        val actual = DispatchScorer.score(listOf(reliable, unreliable))

        assertTrue(actual.getValue(1L) > actual.getValue(2L))
    }

    @Test
    fun `다른 조건이 같으면 오래 기다린 후보가 더 높은 점수를 받는다`() {
        val waitedLong = candidate(riderId = 1L, waitSeconds = 600)
        val justAvailable = candidate(riderId = 2L, waitSeconds = 5)

        val actual = DispatchScorer.score(listOf(waitedLong, justAvailable))

        assertTrue(actual.getValue(1L) > actual.getValue(2L))
    }

    @Test
    fun `거리 40퍼센트가 최근 처리 건수 30퍼센트보다 우선한다`() {
        // A: 거리는 최악이지만 나머지는 최상 / B: 거리는 최상이지만 최근 처리 건수만 최악
        // 거리 가중치(0.4)가 최근 처리 건수 가중치(0.3)보다 커서, 거리에서 진 A가 다른
        // 모든 지표를 이겨도 거리 하나에서 이긴 B를 못 넘는 경우가 생길 수 있음을 검증한다.
        val worseDistanceOnly = candidate(riderId = 1L, distanceMeters = 3000.0, recentDeliveryCount = 0, acceptanceRate = 1.0, waitSeconds = 600)
        val worseRecentCountOnly = candidate(riderId = 2L, distanceMeters = 0.0, recentDeliveryCount = 10, acceptanceRate = 1.0, waitSeconds = 600)

        val actual = DispatchScorer.score(listOf(worseDistanceOnly, worseRecentCountOnly))

        assertTrue(actual.getValue(2L) > actual.getValue(1L))
    }
}
