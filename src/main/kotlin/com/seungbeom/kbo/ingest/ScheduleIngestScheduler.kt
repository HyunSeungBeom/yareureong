package com.seungbeom.kbo.ingest

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.YearMonth

/** 매일 06:00 이번 달 일정/결과를 갱신. (@EnableScheduling 은 메인 클래스에) */
@Component
class ScheduleIngestScheduler(
    private val ingestService: ScheduleIngestService,
    private val asianGamesIngestService: AsianGamesIngestService,
) {
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    fun refreshCurrentMonth() {
        val month = YearMonth.now(SEOUL)
        ingestService.ingestMonth(month)
        asianGamesIngestService.ingestMonth(month)
    }

    private companion object {
        /** 운영 서버는 UTC 라 그냥 now() 를 쓰면 매달 1일 06:00 KST 에 «지난달» 을 수집한다. */
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
