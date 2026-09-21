package com.seungbeom.kbo.team

import com.seungbeom.kbo.league.League
import org.springframework.data.jpa.repository.JpaRepository

interface TeamRepository : JpaRepository<Team, String> {

    /**
     * 그 리그 소속 구단만. **전체 조회를 쓰면 안 된다** — 국가대표팀이 KBO 순위표에 올라온다.
     */
    fun findByLeague(league: League): List<Team>
}
