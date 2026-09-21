package com.seungbeom.kbo.ingest

import com.seungbeom.kbo.game.GameRepository
import com.seungbeom.kbo.league.League
import com.seungbeom.kbo.team.Team
import com.seungbeom.kbo.team.TeamRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

/**
 * 아시안게임 일정/결과를 upsert 한다. KBO 수집([ScheduleIngestService])과 **서비스를 나눈 이유**는
 * 참가팀을 먼저 만들어야 하기 때문이다 — 경기의 외래키가 team 을 가리킨다.
 *
 * 저장되는 경기는 전부 `league = AG` 라서 순위·가을야구 확률 집계에 들어가지 않는다.
 */
@Service
class AsianGamesIngestService(
    private val gameRepository: GameRepository,
    private val teamRepository: TeamRepository,
    private val source: AsianGamesScheduleSource,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun ingestMonth(month: YearMonth): Int {
        val (teams, games) = source.fetch(month)

        /* 이름이 바뀌는 일은 없고 참가국은 대회 중에 늘지 않으므로, 이미 있으면 건드리지 않는다.
           매번 save 하면 대회 기간 내내 10개 행을 의미 없이 갱신한다. */
        teams.filterNot { teamRepository.existsById(it.id) }
            .forEach { teamRepository.save(Team(it.id, it.name, League.ASIAN_GAMES)) }

        for (g in games) {
            val existing = g.externalId?.let { gameRepository.findByExternalId(it) }
            gameRepository.save(ScheduleIngestService.reconcile(existing, g))
        }
        log.info("아시안게임 ingest {} 완료: 참가국 {}팀 · {}경기", month, teams.size, games.size)
        return games.size
    }
}
