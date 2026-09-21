package com.seungbeom.kbo.ingest

import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.team.League
import java.time.LocalDate

/**
 * 외부 출처에서 긁어온 경기 한 건(원시 데이터). DB 엔티티([com.seungbeom.kbo.game.Game])와 분리해서
 * "가져오기"와 "저장"을 느슨하게 결합한다 — 출처가 바뀌어도 upsert 로직은 그대로.
 */
data class ScrapedGame(
    val gameDate: LocalDate,
    val homeTeamId: String,
    val awayTeamId: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: GameStatus = GameStatus.SCHEDULED,
    val homeStartPitcher: String? = null,
    val awayStartPitcher: String? = null,
    val startTime: String? = null,
    val stadium: String? = null,
    /** 출처 경기 id — upsert 키. 없으면 (날짜+홈+원정) 으로 매칭한다. */
    val externalId: String? = null,
    /** 대회 라운드([Rounds]). KBO 는 null. */
    val round: String? = null,
    /** 어느 대회인가. 집계가 섞이면 안 되므로 저장 시점에 반드시 박힌다. */
    val league: String = League.KBO,
)

/**
 * 출처에서 긁어온 참가팀. KBO 구단은 [com.seungbeom.kbo.team.Team] 시드에 고정돼 있지만
 * 국제대회 참가국은 대회마다 달라서 **일정과 함께 들어온다**.
 */
data class ScrapedTeam(
    val id: String,
    val name: String,
)

/** 한 번 긁어온 결과. 참가팀을 먼저 저장해야 경기의 외래키가 성립한다. */
data class ScrapedSchedule(
    val teams: List<ScrapedTeam> = emptyList(),
    val games: List<ScrapedGame> = emptyList(),
)

/**
 * 대회 라운드 코드. 출처 문자열(`agbaseball_group` …)을 그대로 쓰지 않고 우리 값으로 정규화한다 —
 * 출처가 바뀌어도 화면·DB 는 그대로여야 하고, 모르는 코드는 null 로 흘려보내 화면이 안 깨지게 한다.
 */
object Rounds {
    const val GROUP = "GROUP"
    const val SUPER_ROUND = "SUPER_ROUND"
    const val PLACEMENT = "PLACEMENT"
    const val BRONZE = "BRONZE"
    const val FINAL = "FINAL"
}
