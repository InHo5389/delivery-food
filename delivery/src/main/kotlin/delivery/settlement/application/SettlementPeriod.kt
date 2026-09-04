package delivery.settlement.application

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// 정산 기간은 한국 사용자 기준 KST로 경계를 계산한다(OrderService의 일 단위 매출 집계와
// 동일한 이유). 라이더 정산 서비스와 조회 서비스가 공통으로 쓴다.
val KST: ZoneId = ZoneId.of("Asia/Seoul")

// 라이더 정산: 하루 단위(53-6 — 매일 새벽 3시, 전일 DELIVERED 건 집계).
fun dayRange(date: LocalDate): Pair<Instant, Instant> {
    val start = date.atStartOfDay(KST).toInstant()
    val end = date.plusDays(1).atStartOfDay(KST).toInstant()
    return start to end
}

// 상점 정산: 주 단위, 월요일 시작(53-6 — 매주 월요일 새벽 3시, 지난주 월~일 집계).
// weekStart는 반드시 월요일이어야 한다 — 호출부가 임의의 날짜를 넘겨도 항상 그 주의
// 월요일부터 계산한다는 걸 타입/계약으로 명확히 하기 위해 여기서 검증한다.
fun weekRange(weekStart: LocalDate): Pair<Instant, Instant> {
    require(weekStart.dayOfWeek == DayOfWeek.MONDAY) { "주 단위 정산 기간은 월요일부터 시작해야 합니다: $weekStart" }
    val start = weekStart.atStartOfDay(KST).toInstant()
    val end = weekStart.plusWeeks(1).atStartOfDay(KST).toInstant()
    return start to end
}

// 임의의 날짜가 속한 주의 월요일을 구한다 — DayOfWeek는 JDK의 TemporalAdjuster라
// with()에 바로 넘길 수 있다.
fun mondayOf(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)
