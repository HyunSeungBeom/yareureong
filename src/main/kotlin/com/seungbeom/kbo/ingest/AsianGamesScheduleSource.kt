package com.seungbeom.kbo.ingest

import java.time.YearMonth

/**
 * 국제대회(아시안게임) 일정 출처. KBO 출처([KboScheduleSource])와 **인터페이스를 나눈 이유**:
 * 국제대회는 참가국이 대회마다 달라서 일정과 함께 팀 목록도 같이 들어와야 한다
 * (KBO 10구단은 시드로 고정돼 있다).
 */
interface AsianGamesScheduleSource {
    fun fetch(month: YearMonth): ScrapedSchedule
}
