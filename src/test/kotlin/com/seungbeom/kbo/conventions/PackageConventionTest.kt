package com.seungbeom.kbo.conventions

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 백엔드 구조 규약.
 *
 * 프론트에는 `web/tests/conventions.test.ts` 가 있는데 백엔드에는 없어서, 새 패키지가
 * 다른 패키지와 다른 모양으로 자라도 아무도 못 봤다(실제로 `asiangames` 가 그랬다).
 *
 * **KNOWN 과 정확히 일치해야 통과한다.** 새 위반도, 고쳤는데 목록에 남은 항목도 빨개진다.
 * 예외가 필요하면 **이유와 함께** 올린다 — 이유 없이 목록만 늘리는 것은 검사를 끄는 것과 같다.
 */
class PackageConventionTest {

    private val sources: List<File> =
        File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()

    private val controllers = sources.filter { it.name.endsWith("Controller.kt") }

    private fun expectMatchesKnown(hits: List<String>, known: Map<String, String>, message: String) =
        assertEquals(known.keys.sorted(), hits.distinct().sorted(), message)

    @Test
    fun `검사 대상이 실제로 있다`() {
        // 파일을 못 모으면 아래 검사가 전부 «빈 목록끼리» 통과한다.
        assertTrue(sources.size > 20, "소스를 못 모았다: ${sources.size}")
        assertTrue(controllers.size >= 6, "컨트롤러를 못 모았다: ${controllers.map { it.name }}")
    }

    @Test
    fun `컨트롤러 파일에는 컨트롤러만 둔다`() {
        /* 응답 DTO·보조 서비스가 컨트롤러 파일에 얹히면 그 패키지만 «한 파일 덩어리» 가 되고,
           다른 패키지(standings·simulation)처럼 열어 볼 수가 없다. */
        val known = emptyMap<String, String>()
        val hits = controllers.filter { f ->
            Regex("""^(data class|class (?!\w*Controller))""", RegexOption.MULTILINE).containsMatchIn(f.readText())
        }.map { it.name }

        expectMatchesKnown(hits, known, "컨트롤러 파일에 다른 선언이 있다 — 자기 파일로 뺀다")
    }

    @Test
    fun `컨트롤러는 리포지토리를 직접 받지 않는다`() {
        /* 조회 규칙(리그 좁히기 같은)이 컨트롤러로 새면 같은 규칙을 여러 곳에서 되풀이하게 된다. */
        val known = mapOf(
            "BoardController.kt" to "글 권한 판정이 세션·글쓴이·팀을 한꺼번에 봐서 아직 안 나눴다 — 옮길 것",
            "GameController.kt" to "일정 조회가 한 줄이라 서비스를 두지 않았다 — 리그가 하나 더 늘면 옮길 것",
        )
        /* 생성자 구간을 잘라내는 대신 «리포지토리 필드가 있는가» 만 본다 — 위 검사가 컨트롤러 파일에
           다른 클래스를 못 두게 막아 주므로 이 파일의 필드는 곧 컨트롤러의 필드다. */
        val repositoryField = Regex("""val\s+\w+\s*:\s*[\w.]*Repository\b""")
        val hits = controllers.filter { repositoryField.containsMatchIn(it.readText()) }.map { it.name }

        expectMatchesKnown(hits, known, "컨트롤러가 리포지토리를 직접 받는다 — 서비스를 거친다")
    }
}
