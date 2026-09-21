package com.seungbeom.kbo.game

import com.seungbeom.kbo.team.TeamRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import com.seungbeom.kbo.league.League

class InvalidGameFilterException(val errors: List<String>) :
    IllegalArgumentException(errors.joinToString("; "))

/**
 * 조건 검색: 검증 후 [GameFilter.matches] 로 거른다.
 * 한 시즌이 약 720경기라 메모리에서 걸러도 충분하다. 여러 시즌이 쌓이면 JPA Specification 으로 DB에 내린다.
 */
@Service
class GameSearchService(
    private val gameRepository: GameRepository,
    private val teamRepository: TeamRepository,
) {
    fun search(filter: GameFilter): List<Game> {
        val known = teamRepository.findByLeague(League.KBO).mapTo(HashSet()) { it.id }
        val errors = filter.validate(known)
        if (errors.isNotEmpty()) throw InvalidGameFilterException(errors)

        return gameRepository.findByLeagueOrderByGameDateAscIdAsc(League.KBO).filter(filter::matches)
    }
}
