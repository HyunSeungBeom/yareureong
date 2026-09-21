package com.seungbeom.kbo.league

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * **리그를 좁히지 않는 조회를 쓰지 않는다.**
 *
 * 국제대회 경기가 한 테이블에 함께 들어오므로, `findAll()` 같은 전체 조회 한 번이면 KBO 순위·
 * 우승확률이 조용히 틀어진다(에러가 안 난다). 목으로 검증하면 «그 테스트가 다루는 경로» 만
 * 지켜지므로, 소스를 직접 훑어 **새로 생기는 호출까지** 막는다.
 *
 * 저장소 인터페이스 자체에서 리그 없는 메서드를 없앴기 때문에 대부분은 컴파일 단계에서 걸린다.
 * 여기서는 `findAll()` 처럼 JpaRepository 가 기본 제공하는 것들을 본다.
 */
class LeagueQueryConventionTest {

    private val forbidden = listOf("gameRepository.findAll(", "teamRepository.findAll(", "teams.findAll(", "games.findAll(")

    private fun mainSources(): List<File> =
        File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()

    @Test
    fun `제품 코드가 전체 조회를 쓰지 않는다`() {
        val hits = mainSources().flatMap { file ->
            val body = file.readText()
            forbidden.filter { body.contains(it) }.map { "${file.path} → $it" }
        }

        assertTrue(hits.isEmpty(), "리그를 좁히지 않는 조회가 있다:\n${hits.joinToString("\n")}")
    }

    @Test
    fun `검사 대상이 실제로 있다 — 파일을 못 모으면 위 검사가 빈 채로 통과한다`() {
        assertTrue(mainSources().size > 20, "소스를 못 모았다: ${mainSources().size}개")
    }
}
