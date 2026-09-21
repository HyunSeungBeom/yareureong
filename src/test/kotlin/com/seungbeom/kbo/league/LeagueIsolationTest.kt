package com.seungbeom.kbo.league

import com.seungbeom.kbo.game.Game
import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.prediction.PredictionService
import com.seungbeom.kbo.simulation.SimulationService
import com.seungbeom.kbo.standings.StandingsService
import com.seungbeom.kbo.league.League
import com.seungbeom.kbo.team.Team
import com.seungbeom.kbo.team.TeamRepository
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * **리그 격리** — 국제대회(아시안게임) 경기가 KBO 집계에 섞이지 않는지.
 *
 * 섞여도 에러가 나지 않는다. 순위표에 «대한민국» 이 뜨거나 우승 확률이 조용히 틀어질 뿐이다.
 * 그래서 «리그를 좁히지 않는 조회를 썼는가» 를 호출 수준에서 못 박는다.
 */
class LeagueIsolationTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = ArgumentMatchers.any<T>() as T

    private val games = mock(GameRepository::class.java)
    private val teams = mock(TeamRepository::class.java)
    private val standings = StandingsService(teams, games)

    private val today = LocalDate.of(2026, 9, 21)

    private fun kboGame(home: String, away: String, hs: Int?, aws: Int?) = Game(
        id = 1, gameDate = today, homeTeamId = home, awayTeamId = away,
        homeScore = hs, awayScore = aws, status = if (hs == null) GameStatus.SCHEDULED else GameStatus.FINAL,
        league = League.KBO,
    )

    @Test
    fun `순위는 KBO 구단과 KBO 경기만 본다`() {
        given(teams.findByLeague(League.KBO)).willReturn(listOf(Team("OB", "두산 베어스"), Team("LG", "LG 트윈스")))
        given(games.findByLeagueAndStatus(League.KBO, GameStatus.FINAL))
            .willReturn(listOf(kboGame("OB", "LG", 5, 3)))

        val rows = standings.standings()

        assertEquals(2, rows.size, "국가대표팀이 순위표에 올라왔다")
        /* 팀 수만 보면 «KBO 아닌 리그를 조회» 해도 통과한다(빈 결과 + 0승 0패) — 결과 내용까지 본다 */
        val doosan = rows.single { it.teamId == "OB" }
        assertEquals(1, doosan.wins, "KBO 종료 경기가 순위에 반영되지 않았다")
        assertEquals(1, rows.single { it.teamId == "LG" }.losses)
        verify(teams).findByLeague(League.KBO)
        verify(teams, never()).findAll()
    }

    /** 시뮬레이터는 포스트시즌 구조상 5팀 이상을 요구한다 — KBO 10구단을 그대로 준다 */
    private val kboTeams = listOf("OB", "LG", "SS", "KT", "SK", "WO", "HH", "LT", "HT", "NC")
        .map { Team(it, it) }

    @Test
    fun `시뮬레이션은 KBO 잔여 경기만 본다 — 국제대회가 남은 일정을 부풀리면 확률이 틀어진다`() {
        given(teams.findByLeague(League.KBO)).willReturn(kboTeams)
        given(games.findByLeagueAndStatus(League.KBO, GameStatus.FINAL)).willReturn(listOf(kboGame("OB", "LG", 5, 3)))
        given(games.findByLeagueAndStatus(League.KBO, GameStatus.SCHEDULED)).willReturn(emptyList())

        SimulationService(standings, games).simulate(10)

        verify(games).findByLeagueAndStatus(League.KBO, GameStatus.SCHEDULED)
    }

    @Test
    fun `오늘 경기 예측은 KBO 경기만 가져온다`() {
        given(teams.findByLeague(League.KBO)).willReturn(kboTeams)
        given(games.findByLeagueAndStatus(League.KBO, GameStatus.FINAL)).willReturn(emptyList())
        given(games.findByLeagueAndGameDateOrderByIdAsc(League.KBO, today)).willReturn(emptyList())

        val result = PredictionService(standings, games).gamesOn(today)

        assertTrue(result.isEmpty())
        verify(games).findByLeagueAndGameDateOrderByIdAsc(League.KBO, today)
    }

    @Test
    fun `리그 상수는 서로 다르다 — 같은 값이면 격리가 무의미하다`() {
        assertTrue(League.KBO != League.ASIAN_GAMES)
    }
}
