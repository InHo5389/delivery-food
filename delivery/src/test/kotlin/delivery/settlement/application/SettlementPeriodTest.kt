package delivery.settlement.application

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class SettlementPeriodTest {

    private val zone = ZoneId.of("Asia/Seoul")

    @Test
    fun `dayRange는 그날 00시부터 다음날 00시 직전까지다`() {
        val date = LocalDate.of(2026, 3, 15)

        val (start, end) = dayRange(date)

        assertEquals(date.atStartOfDay(zone).toInstant(), start)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), end)
    }

    @Test
    fun `weekRange는 월요일 00시부터 다음 월요일 00시 직전까지다`() {
        val monday = LocalDate.of(2026, 3, 9)

        val (start, end) = weekRange(monday)

        assertEquals(monday.atStartOfDay(zone).toInstant(), start)
        assertEquals(monday.plusWeeks(1).atStartOfDay(zone).toInstant(), end)
    }

    @Test
    fun `weekRange에 월요일이 아닌 날짜를 넘기면 예외가 발생한다`() {
        val tuesday = LocalDate.of(2026, 3, 10)

        assertThrows<IllegalArgumentException> { weekRange(tuesday) }
    }

    @Test
    fun `mondayOf는 주중 아무 날짜를 넘겨도 그 주의 월요일을 반환한다`() {
        val monday = LocalDate.of(2026, 3, 9)
        val sunday = LocalDate.of(2026, 3, 15)

        assertEquals(monday, mondayOf(monday))
        assertEquals(monday, mondayOf(sunday))
        assertEquals(monday, mondayOf(LocalDate.of(2026, 3, 12)))
    }
}
