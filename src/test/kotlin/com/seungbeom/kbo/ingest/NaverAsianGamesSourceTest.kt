package com.seungbeom.kbo.ingest

import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.team.League
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 네이버 응답 파싱 테스트. 픽스처(`naver-asian-games-sample.json`)는 2026 아시안게임 실제 응답에서
 * 7건을 잘라낸 것이다 — 예선 예정 1, **종료로 가공한 1**, 취소로 가공한 1, 조 미정 슬롯 2,
 * 그리고 같은 응답에 섞여 오는 **KBO 경기 1 · 국내야구기타 1**.
 *
 * 대회 시작 전이라 종료·취소 경기는 실제 응답에 없어서 두 건을 손으로 바꿨다. 나머지 필드는
 * 실응답 그대로라 출처가 형태를 바꾸면 여기가 먼저 깨진다.
 */
class NaverAsianGamesSourceTest {

    private val sample: String =
        javaClass.getResource("/naver-asian-games-sample.json")!!.readText()

    private fun parsed() = NaverAsianGamesSource.parse(sample)

    @Test
    fun `아시안게임 경기만 남긴다 - KBO 가 섞여 들어오면 순위가 오염된다`() {
        val games = parsed().games

        // 취소 1 · 조 미정 2 · KBO 1 · 국내야구기타 1 이 빠지고 2경기.
        assertEquals(2, games.size)
        assertTrue(games.all { it.league == League.ASIAN_GAMES })
        // KBO 경기의 팀 코드도 두 글자라 «모양» 만으로는 못 거른다 — 대회 구분으로 걸러야 한다.
        assertTrue(games.none { it.externalId == "20260919HHLG02026" }, "KBO 경기가 섞였다")
    }

    @Test
    fun `예정 경기는 점수를 비우고 시간·구장·라운드를 채운다`() {
        val g = parsed().games.single { it.externalId == "88880921TWKR02026" }

        assertEquals(LocalDate.of(2026, 9, 21), g.gameDate)
        assertEquals("AG-KR", g.homeTeamId)
        assertEquals("AG-TW", g.awayTeamId)
        assertEquals(GameStatus.SCHEDULED, g.status)
        // 예정 경기도 점수가 0 으로 내려온다. 그대로 저장하면 «0대0 무승부» 로 보인다.
        assertNull(g.homeScore)
        assertNull(g.awayScore)
        assertEquals("18:30", g.startTime)
        assertEquals("오카자키", g.stadium)
        assertEquals(Rounds.GROUP, g.round)
    }

    @Test
    fun `종료 경기는 FINAL 과 점수를 채운다`() {
        val g = parsed().games.single { it.externalId == "88880921JPCN02026" }

        assertEquals(GameStatus.FINAL, g.status)
        assertEquals("AG-CN", g.homeTeamId)
        assertEquals(3, g.homeScore)
        assertEquals(7, g.awayScore)
    }

    @Test
    fun `조 편성 전 슬롯은 버린다 - 가짜 팀이 생기면 안 된다`() {
        val schedule = parsed()

        // A1 vs B2(결승) · A3 vs B3(순위결정전) 은 아직 «팀» 이 아니다.
        assertTrue(schedule.games.none { it.round == Rounds.FINAL || it.round == Rounds.PLACEMENT })
        assertTrue(
            schedule.teams.none { it.id.any(Char::isDigit) },
            "조 슬롯이 팀으로 저장됐다: ${schedule.teams.map { it.id }}",
        )
    }

    @Test
    fun `참가팀은 살아남은 경기에 나온 나라만 모은다`() {
        val teams = parsed().teams

        assertEquals(setOf("AG-KR", "AG-TW", "AG-JP", "AG-CN"), teams.map { it.id }.toSet())
        assertEquals("대한민국", teams.single { it.id == "AG-KR" }.name)
        // 취소된 한국-홍콩 경기의 홍콩은 들어오지 않는다.
        assertTrue(teams.none { it.id == "AG-HK" })
    }

    @Test
    fun `응답이 비거나 깨져도 예외 없이 빈 결과를 준다`() {
        // 문서 없는 API 라 언제든 형태가 바뀔 수 있다. 수집이 터지면 다른 리그까지 못 갱신한다.
        for (body in listOf(null, "", "not json", """{"result":{}}""", """{"result":{"games":"?"}}""")) {
            val schedule = NaverAsianGamesSource.parse(body)
            assertTrue(schedule.games.isEmpty() && schedule.teams.isEmpty(), "body=$body")
        }
    }

    @Test
    fun `조회 주소는 그 달 1일부터 말일까지다`() {
        val url = NaverAsianGamesSource.buildUrl(YearMonth.of(2026, 9))

        assertTrue(url.contains("fromDate=2026-09-01"), url)
        assertTrue(url.contains("toDate=2026-09-30"), url)
        assertTrue(url.contains("upperCategoryId=kbaseball"), url)
    }

    @Test
    fun `구장·라운드 필드를 명시해서 요청한다`() {
        // 기본 응답에는 stadium·roundCode 가 아예 없다. 빼먹으면 «파싱은 성공하는데 값만 null» 이
        // 되어 화면에서야 알게 된다 — 실제로 밟았던 함정이다.
        val url = NaverAsianGamesSource.buildUrl(YearMonth.of(2026, 9))

        assertTrue(url.contains("stadium"), url)
        assertTrue(url.contains("roundCode"), url)
    }
}
