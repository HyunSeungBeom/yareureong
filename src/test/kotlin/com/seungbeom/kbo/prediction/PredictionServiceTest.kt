package com.seungbeom.kbo.prediction

import com.seungbeom.kbo.game.Game
import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.standings.StandingsService
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.seungbeom.kbo.team.League

/** 그날 경기 + 예측 조립. 승률·등판 이력은 목으로 주고 «무엇이 확률을 움직이나» 를 본다. */
class PredictionServiceTest {

    private val today = LocalDate.of(2026, 9, 18)
    private val standings = mock(StandingsService::class.java)
    private val games = mock(GameRepository::class.java)
    private val service = PredictionService(standings, games)

    private var seq = 0L
    private fun game(
        home: String, away: String,
        homePitcher: String? = null, awayPitcher: String? = null,
        homeScore: Int? = null, awayScore: Int? = null,
        date: LocalDate = today,
    ) = Game(
        id = ++seq, gameDate = date, homeTeamId = home, awayTeamId = away,
        homeScore = homeScore, awayScore = awayScore,
        status = if (homeScore == null) GameStatus.SCHEDULED else GameStatus.FINAL,
        homeStartPitcher = homePitcher, awayStartPitcher = awayPitcher,
    )

    private fun setUp(todayGames: List<Game>, history: List<Game> = emptyList()) {
        given(standings.winPctByTeam()).willReturn(mapOf("OB" to 0.520, "LG" to 0.567))
        given(games.findByLeagueAndGameDateOrderByIdAsc(League.KBO, today)).willReturn(todayGames)
        given(games.findByLeagueAndStatus(League.KBO, GameStatus.FINAL)).willReturn(history)
    }

    @Test
    fun `그날 경기가 없으면 빈 목록이다`() {
        given(games.findByLeagueAndGameDateOrderByIdAsc(League.KBO, today)).willReturn(emptyList())

        assertEquals(emptyList(), service.gamesOn(today))
    }

    @Test
    fun `경기 수만큼 예측을 돌려주고 확률 합은 1 이다`() {
        setUp(listOf(game("OB", "LG"), game("LG", "OB")))

        val result = service.gamesOn(today)

        assertEquals(2, result.size)
        result.forEach { assertEquals(1.0, it.homeWinProb + it.awayWinProb, 1e-9) }
    }

    @Test
    fun `첫 선발이면 팀 승률만 쓴 예측과 정확히 같다 — 데이터가 없으면 아무 말도 하지 않는다`() {
        setUp(listOf(game("OB", "LG", homePitcher = "신인", awayPitcher = "신인2")))

        val withStarters = service.gamesOn(today).single()

        assertEquals(Predictions.homeWinProbability(0.520, 0.567), withStarters.homeWinProb, 1e-9)
        assertTrue(withStarters.home.firstStart)
        assertTrue(withStarters.away.firstStart)
    }

    @Test
    fun `선발이 아예 없는(미발표) 경기도 팀 승률로 계산한다`() {
        setUp(listOf(game("OB", "LG")))

        val result = service.gamesOn(today).single()

        assertEquals(Predictions.homeWinProbability(0.520, 0.567), result.homeWinProb, 1e-9)
        assertEquals(null, result.home.name)
    }

    @Test
    fun `좋은 선발이 나오면 그 팀 승리확률이 올라간다`() {
        val history = (1..20).map { game("OB", "HH", homePitcher = "에이스", homeScore = 5, awayScore = 1) }
        setUp(listOf(game("OB", "LG", homePitcher = "에이스")), history)

        val result = service.gamesOn(today).single()

        assertTrue(
            result.homeWinProb > Predictions.homeWinProbability(0.520, 0.567),
            "선발 보정이 반영되지 않았다: ${result.homeWinProb}",
        )
        assertEquals(20, result.home.starts)
        assertEquals(20, result.home.wins)
    }

    @Test
    fun `상대전적은 표시용으로 함께 내려간다`() {
        val history = listOf(
            game("OB", "LG", homePitcher = "곽빈", homeScore = 5, awayScore = 3),
            game("OB", "HH", homePitcher = "곽빈", homeScore = 1, awayScore = 4),
        )
        setUp(listOf(game("OB", "LG", homePitcher = "곽빈")), history)

        val result = service.gamesOn(today).single()

        assertEquals(StarterRecord(starts = 1, wins = 1, losses = 0), result.home.vsOpponent)
        assertEquals(2, result.home.starts)
    }

    @Test
    fun `종료된 경기는 점수와 함께 내려간다`() {
        setUp(listOf(game("OB", "LG", homeScore = 7, awayScore = 3)))

        val result = service.gamesOn(today).single()

        assertEquals(GameStatus.FINAL, result.status)
        assertEquals(7, result.homeScore)
    }
}
