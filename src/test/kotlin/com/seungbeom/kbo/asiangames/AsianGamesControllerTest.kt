package com.seungbeom.kbo.asiangames

import com.seungbeom.kbo.game.Game
import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.ingest.Rounds
import com.seungbeom.kbo.team.League
import com.seungbeom.kbo.team.Team
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AsianGamesControllerTest {

    private val date = LocalDate.of(2026, 9, 21)
    private val korea = Team("AG-KR", "대한민국", League.ASIAN_GAMES)
    private val taipei = Team("AG-TW", "차이니스 타이베이", League.ASIAN_GAMES)

    private fun game(home: String, away: String, hs: Int? = null, aws: Int? = null) = Game(
        id = 1, gameDate = date, homeTeamId = home, awayTeamId = away,
        homeScore = hs, awayScore = aws,
        status = if (hs == null) GameStatus.SCHEDULED else GameStatus.FINAL,
        startTime = "18:30", stadium = "오카자키", round = Rounds.GROUP,
        league = League.ASIAN_GAMES,
    )

    @Test
    fun `국가 이름과 코드를 붙여 내려준다`() {
        val v = AsianGamesController.views(listOf(game("AG-KR", "AG-TW", 5, 2)), listOf(korea, taipei)).single()

        assertEquals("대한민국", v.home.name)
        assertEquals("KR", v.home.code, "국기를 고르려면 접두사 없는 코드가 필요하다")
        assertEquals("차이니스 타이베이", v.away.name)
        assertEquals(5, v.homeScore)
        assertEquals(Rounds.GROUP, v.round)
        assertEquals("오카자키", v.stadium)
    }

    @Test
    fun `모르는 참가국이어도 경기를 버리지 않는다`() {
        // 참가국 저장이 한 건 실패해도 그날 일정이 통째로 사라지면 안 된다.
        val v = AsianGamesController.views(listOf(game("AG-KR", "AG-XX")), listOf(korea)).single()

        assertEquals("XX", v.away.name)
        assertEquals("XX", v.away.code)
    }
}
