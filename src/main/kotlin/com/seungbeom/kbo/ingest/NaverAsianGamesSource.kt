package com.seungbeom.kbo.ingest

import com.seungbeom.kbo.game.GameStatus
import com.seungbeom.kbo.game.Round
import com.seungbeom.kbo.league.League
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.YearMonth

/**
 * 아시안게임 야구 일정 어댑터 — 네이버 스포츠 일정 API(`api-gw.sports.naver.com`).
 *
 * 다음(daum)에는 국제대회 리그 코드가 없어서 출처를 따로 뚫었다. 역시 **문서 없는 비공식 API**라
 * [DaumKboScheduleSource] 와 같은 방어를 한다 — fetch 실패는 빈 결과로 흡수하고 파싱은 전부 null-safe.
 *
 * **`live` 프로필로 가두지 않는다.** 다음 출처를 가둔 이유는 시드 출처가 실제 결과를 가짜 점수로
 * 덮어쓰기 때문인데, 아시안게임은 시드 상대가 아예 없어서 그 사고가 일어날 수 없다.
 */
@Component
class NaverAsianGamesSource(
    private val restClient: RestClient = RestClient.create(),
) : AsianGamesScheduleSource {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetch(month: YearMonth): ScrapedSchedule {
        val url = buildUrl(month)
        return try {
            val body = restClient.get()
                .uri(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://m.sports.naver.com/")
                .retrieve()
                .body(String::class.java)
            parse(body).also { log.info("아시안게임 일정 {}건 수신 ({})", it.games.size, month) }
        } catch (e: Exception) {
            log.warn("네이버 아시안게임 fetch 실패({}): {}", url, e.message)
            ScrapedSchedule()
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

        /** 아시안게임 경기만 골라내는 값. 같은 응답에 kbo·국내야구기타가 섞여 온다. */
        private const val CATEGORY = "agbaseball"

        /**
         * 참가국 코드는 두 글자(KR·JP·TW…)이고, 조 편성이 안 끝난 슬롯은 숫자가 섞인다(A1·B3·S2).
         * **국가 목록을 하드코딩하지 않는 이유**: 대회마다 참가국이 바뀌는데 목록을 못 따라가면
         * 그 나라 경기가 조용히 사라진다. «모양» 으로 가리면 새 참가국도 그냥 들어온다.
         */
        private val COUNTRY_CODE = Regex("^[A-Z]{2}$")

        private val ROUND_BY_CODE = mapOf(
            "${CATEGORY}_group" to Round.GROUP,
            "${CATEGORY}_super_round" to Round.SUPER_ROUND,
            "${CATEGORY}_placement" to Round.PLACEMENT,
            "${CATEGORY}_bronze" to Round.BRONZE,
            "${CATEGORY}_final" to Round.FINAL,
        )

        private val mapper = ObjectMapper()

        /** 국가 코드를 그대로 팀 id 로 쓰면 KBO 코드(`KT`·`LG`)와 부딪힌다. 대회 접두사를 붙인다. */
        const val ID_PREFIX = "AG-"

        fun teamId(countryCode: String): String = ID_PREFIX + countryCode

        fun buildUrl(month: YearMonth): String =
            "https://api-gw.sports.naver.com/schedule/games" +
                "?upperCategoryId=kbaseball" +
                "&fromDate=${month.atDay(1)}" +
                "&toDate=${month.atEndOfMonth()}" +
                "&size=500" +
                /* 이게 없으면 구장·라운드가 **응답에서 통째로 빠진다**(에러가 아니라 필드 부재라
                   조용히 null 로 저장된다). 실제로 한 번 밟았다 — 그래서 테스트로 박아둔다. */
                "&fields=basic,stadium,roundCode"

        /**
         * 순수 함수: 응답 JSON → 참가팀 + 경기. 네트워크 없이 테스트한다.
         * 응답 형태: `{ result: { games: [ { categoryId, gameId, homeTeamCode, ... } ] } }`
         */
        fun parse(body: String?): ScrapedSchedule {
            if (body.isNullOrBlank()) return ScrapedSchedule()
            val games = runCatching { mapper.readTree(body) }.getOrNull()
                ?.path("result")?.path("games")
                ?.takeIf { it.isArray } ?: return ScrapedSchedule()

            val teams = LinkedHashMap<String, ScrapedTeam>()
            val parsed = games.mapNotNull { node ->
                val game = parseGame(node) ?: return@mapNotNull null
                /* 팀 id 를 경기에서 그대로 가져온다. 노드를 따로 읽어 만들면 «경기는 가리키는데
                   목록에 없는 팀» 이 생길 수 있고, 그러면 외래키 위반으로 그 달 수집이 통째로
                   롤백된다 — 한 경기만 빠지는 게 아니다. */
                teams[game.homeTeamId] = ScrapedTeam(game.homeTeamId, teamName(node, "home", game.homeTeamId))
                teams[game.awayTeamId] = ScrapedTeam(game.awayTeamId, teamName(node, "away", game.awayTeamId))
                game
            }
            return ScrapedSchedule(teams.values.toList(), parsed)
        }

        /** 이름이 비면 국가 코드를 이름 자리에 쓴다 — 화면도 같은 방식으로 버틴다. */
        private fun teamName(node: JsonNode, side: String, teamId: String): String =
            node.path("${side}TeamName").asString(null)?.takeIf { it.isNotBlank() }
                ?: teamId.removePrefix(ID_PREFIX)

        private fun parseGame(node: JsonNode): ScrapedGame? {
            if (node.path("categoryId").asString(null) != CATEGORY) return null
            if (node.path("cancel").asBoolean(false)) return null

            val home = node.path("homeTeamCode").asString(null) ?: return null
            val away = node.path("awayTeamCode").asString(null) ?: return null
            /* 조 편성 전 슬롯(A1 vs B3)은 «팀» 이 아니다. 저장하면 team 테이블에 가짜 구단이 생기고
               순위·게시판 같은 팀 기준 화면이 그걸 팀으로 취급하게 된다. 확정되면 같은 경기 id 에
               국가 코드가 채워지므로 다음 수집 때 자연스럽게 들어온다. */
            if (!COUNTRY_CODE.matches(home) || !COUNTRY_CODE.matches(away)) return null

            val date = node.path("gameDate").asString(null)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
            val final = node.path("statusCode").asString(null) == "RESULT"

            return ScrapedGame(
                gameDate = date,
                homeTeamId = teamId(home),
                awayTeamId = teamId(away),
                /* 예정 경기의 점수는 0 으로 내려온다 — 0:0 으로 저장하면 «0대0 무승부» 처럼 보인다. */
                homeScore = if (final) node.path("homeTeamScore").asInt(0) else null,
                awayScore = if (final) node.path("awayTeamScore").asInt(0) else null,
                status = if (final) GameStatus.FINAL else GameStatus.SCHEDULED,
                startTime = node.path("gameDateTime").asString(null)
                    ?.takeIf { it.length >= 16 }?.substring(11, 16),
                stadium = node.path("stadium").asString(null)?.takeIf { it.isNotBlank() },
                externalId = node.path("gameId").asString(null) ?: return null,
                round = ROUND_BY_CODE[node.path("roundCode").asString(null)],
                league = League.ASIAN_GAMES,
            )
        }
    }
}
