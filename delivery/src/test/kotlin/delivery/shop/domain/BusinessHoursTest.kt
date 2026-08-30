package delivery.shop.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusinessHoursTest {

    @Test
    fun `시작 시각과 종료 시각이 같으면 생성에 실패한다`() {
        assertThrows<IllegalArgumentException> {
            BusinessHours(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 0))
        }
    }

    @Test
    fun `자정을 넘기지 않는 일반적인 영업시간 범위 안이면 true를 반환한다`() {
        val hours = BusinessHours(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(22, 0))

        assertTrue(hours.isWithin(LocalTime.of(9, 0)))
        assertTrue(hours.isWithin(LocalTime.of(21, 59)))
        assertFalse(hours.isWithin(LocalTime.of(22, 0)))
        assertFalse(hours.isWithin(LocalTime.of(8, 59)))
    }

    @Test
    fun `자정을 넘기는 영업시간에서 자정 이전 시각은 영업중으로 판단한다`() {
        val hours = BusinessHours(1L, DayOfWeek.MONDAY, LocalTime.of(22, 0), LocalTime.of(2, 0))

        assertTrue(hours.isWithin(LocalTime.of(23, 59)))
        assertTrue(hours.isWithin(LocalTime.of(22, 0)))
    }

    @Test
    fun `자정을 넘기는 영업시간에서 자정 이후 새벽 시각도 영업중으로 판단한다`() {
        val hours = BusinessHours(1L, DayOfWeek.MONDAY, LocalTime.of(22, 0), LocalTime.of(2, 0))

        assertTrue(hours.isWithin(LocalTime.of(0, 0)))
        assertTrue(hours.isWithin(LocalTime.of(1, 59)))
        assertFalse(hours.isWithin(LocalTime.of(2, 0)))
    }

    @Test
    fun `자정을 넘기는 영업시간에서 영업시간 바깥 낮 시각은 영업중이 아니다`() {
        val hours = BusinessHours(1L, DayOfWeek.MONDAY, LocalTime.of(22, 0), LocalTime.of(2, 0))

        assertFalse(hours.isWithin(LocalTime.of(12, 0)))
        assertFalse(hours.isWithin(LocalTime.of(21, 59)))
    }
}
