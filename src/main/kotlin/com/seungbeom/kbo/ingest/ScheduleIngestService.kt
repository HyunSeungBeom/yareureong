package com.seungbeom.kbo.ingest

import com.seungbeom.kbo.game.Game
import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.game.GameStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

/**
 * 출처에서 한 달치를 가져와 DB에 **upsert**한다(멱등: 여러 번 돌려도 중복 안 생김).
 * 예정 경기는 나중에 결과가 나오면 같은 레코드가 점수·상태만 갱신된다.
 */
@Service
class ScheduleIngestService(
    private val gameRepository: GameRepository,
    private val source: KboScheduleSource,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun ingestMonth(month: YearMonth): Int {
        val scraped = source.fetch(month)
        for (s in scraped) {
            gameRepository.save(reconcile(findExisting(s), s))
        }
        log.info("ingest {} 완료: {}건 반영", month, scraped.size)
        return scraped.size
    }

    /**
     * 같은 경기를 찾는다. 출처 id 가 있으면 그것으로 — (날짜+홈+원정) 은 **더블헤더에서 충돌**해
     * 두 경기가 한 건으로 덮어써진다. id 가 없던 시절에 들어온 행을 위해 옛 키로도 찾는다.
     */
    private fun findExisting(s: ScrapedGame): Game? =
        s.externalId?.let { gameRepository.findByExternalId(it) }
            ?: gameRepository.findByGameDateAndHomeTeamIdAndAwayTeamId(s.gameDate, s.homeTeamId, s.awayTeamId)
                ?.takeIf { it.externalId == null }

    companion object {
        /**
         * 순수 함수: 기존 레코드(없으면 null)와 긁어온 데이터로 저장할 Game 을 만든다.
         * Spring/DB 없이 단독 테스트 가능 — 여기가 스크래퍼의 진짜 로직.
         */
        fun reconcile(existing: Game?, s: ScrapedGame): Game {
            val game = existing ?: Game(
                gameDate = s.gameDate,
                homeTeamId = s.homeTeamId,
                awayTeamId = s.awayTeamId,
                league = s.league,
            )
            game.homeScore = s.homeScore
            game.awayScore = s.awayScore
            game.status = s.status
            game.externalId = s.externalId ?: game.externalId

            /* 선발은 경기 당일까지 바뀌므로 예정 경기면 새 값으로 갈아끼운다. 다만 **새 값이 없을 때
               기존 값을 지우지는 않는다** — 발표된 선발이 재수집 한 번에 사라지면 화면이 비어 버린다.
               종료 경기의 선발은 확정값이라 덮어쓰지 않는다. */
            if (game.status != GameStatus.FINAL || game.homeStartPitcher == null) {
                game.homeStartPitcher = s.homeStartPitcher ?: game.homeStartPitcher
            }
            if (game.status != GameStatus.FINAL || game.awayStartPitcher == null) {
                game.awayStartPitcher = s.awayStartPitcher ?: game.awayStartPitcher
            }
            game.round = s.round ?: game.round
            game.startTime = s.startTime ?: game.startTime
            game.stadium = s.stadium ?: game.stadium
            return game
        }
    }
}
