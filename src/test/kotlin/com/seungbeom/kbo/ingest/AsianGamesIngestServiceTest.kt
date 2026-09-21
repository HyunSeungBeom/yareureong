package com.seungbeom.kbo.ingest

import com.seungbeom.kbo.game.Game
import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.team.League
import com.seungbeom.kbo.team.Team
import com.seungbeom.kbo.team.TeamRepository
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 아시안게임 수집 서비스. **참가국을 먼저 만들어야** 경기의 외래키가 성립하고,
 * 저장되는 경기는 전부 `league = AG` 여야 KBO 순위·확률에 섞이지 않는다.
 */
class AsianGamesIngestServiceTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = ArgumentMatchers.any<T>() as T

    private val games = mock(GameRepository::class.java)
    private val teams = mock(TeamRepository::class.java)

    private val korea = ScrapedTeam("AG-KR", "대한민국")
    private val japan = ScrapedTeam("AG-JP", "일본")
    private val game = ScrapedGame(
        gameDate = LocalDate.of(2026, 9, 21),
        homeTeamId = "AG-KR",
        awayTeamId = "AG-JP",
        startTime = "18:30",
        externalId = "88880921JPKR02026",
        round = Rounds.GROUP,
        league = League.ASIAN_GAMES,
    )

    private fun ingest(schedule: ScrapedSchedule): Int {
        val source = object : AsianGamesScheduleSource {
            override fun fetch(month: YearMonth) = schedule
        }
        given(games.save(anyArg<Game>())).willAnswer { it.arguments[0] }
        return AsianGamesIngestService(games, teams, source).ingestMonth(YearMonth.of(2026, 9))
    }

    @Test
    fun `참가국을 AG 리그로 만들고 경기를 저장한다`() {
        given(teams.existsById(anyArg())).willReturn(false)

        assertEquals(1, ingest(ScrapedSchedule(listOf(korea, japan), listOf(game))))

        val saved = ArgumentCaptor.forClass(Team::class.java)
        verify(teams, org.mockito.Mockito.times(2)).save(saved.capture())
        assertEquals(listOf("AG-KR", "AG-JP"), saved.allValues.map { it.id })
        assertTrue(saved.allValues.all { it.league == League.ASIAN_GAMES }, "국가대표팀이 KBO 로 저장됐다")
    }

    @Test
    fun `이미 있는 참가국은 다시 저장하지 않는다`() {
        // 대회 기간 내내 매일 수집하는데 참가국은 안 바뀐다.
        given(teams.existsById(anyArg())).willReturn(true)

        ingest(ScrapedSchedule(listOf(korea, japan), listOf(game)))

        verify(teams, never()).save(anyArg<Team>())
    }

    @Test
    fun `저장되는 경기에 AG 리그와 라운드가 박힌다`() {
        given(teams.existsById(anyArg())).willReturn(true)

        ingest(ScrapedSchedule(listOf(korea, japan), listOf(game)))

        val saved = ArgumentCaptor.forClass(Game::class.java)
        verify(games).save(saved.capture())
        assertEquals(League.ASIAN_GAMES, saved.value.league, "KBO 집계에 국제대회가 섞인다")
        assertEquals(Rounds.GROUP, saved.value.round)
        assertEquals("88880921JPKR02026", saved.value.externalId)
    }

    @Test
    fun `같은 경기를 다시 수집하면 새로 만들지 않고 갱신한다`() {
        given(teams.existsById(anyArg())).willReturn(true)
        val existing = Game(
            id = 7, gameDate = game.gameDate, homeTeamId = "AG-KR", awayTeamId = "AG-JP",
            externalId = game.externalId, league = League.ASIAN_GAMES,
        )
        given(games.findByExternalId(game.externalId!!)).willReturn(existing)

        ingest(ScrapedSchedule(emptyList(), listOf(game.copy(status = GameStatus.FINAL, homeScore = 5, awayScore = 2))))

        val saved = ArgumentCaptor.forClass(Game::class.java)
        verify(games).save(saved.capture())
        assertEquals(7, saved.value.id, "경기가 중복 저장됐다")
        assertEquals(5, saved.value.homeScore)
    }
}
