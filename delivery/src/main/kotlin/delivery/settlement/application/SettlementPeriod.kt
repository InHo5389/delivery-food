package delivery.settlement.application

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

// 정산 기간은 "그 달" 단위라 한국 사용자 기준 KST로 월 경계를 계산한다(OrderService의
// 일 단위 매출 집계와 동일한 이유). 상점/라이더 정산 서비스와 조회 서비스가 공통으로 쓴다.
val KST: ZoneId = ZoneId.of("Asia/Seoul")

fun monthRange(yearMonth: YearMonth): Pair<Instant, Instant> {
    val start = yearMonth.atDay(1).atStartOfDay(KST).toInstant()
    val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(KST).toInstant()
    return start to end
}
