package com.seungbeom.kbo.ingest

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.time.YearMonth

/**
 * 수집 트리거. 예: `POST /api/admin/ingest?month=2026-07` + `X-Admin-Token` 헤더.
 *
 * 운영에서는 GitHub Actions 크론(`ingest.yml`)이 매일 부른다 — 무료 서버가 잠들어 있으면 앱 내부
 * `@Scheduled` 가 돌지 않기 때문이다. 공개된 주소라 **토큰 없이는 누구도 스크래핑을 반복 실행시킬 수
 * 없어야** 한다.
 */
@RestController
class IngestController(
    private val ingestService: ScheduleIngestService,
    private val asianGamesIngestService: AsianGamesIngestService,
    @Value("\${app.admin.token:}") private val adminToken: String,
) {
    @PostMapping("/api/admin/ingest")
    fun ingest(
        @RequestHeader(ADMIN_TOKEN_HEADER, required = false) token: String?,
        @RequestParam month: String,
    ): ResponseEntity<Map<String, Any>> {
        if (!authorized(adminToken, token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val count = ingestService.ingestMonth(YearMonth.parse(month))
        return ResponseEntity.ok(mapOf("month" to month, "ingested" to count))
    }

    /** 아시안게임은 출처가 달라 엔드포인트를 나눈다. 대회 기간이 아니면 0건이 반영된다. */
    @PostMapping("/api/admin/ingest/asian-games")
    fun ingestAsianGames(
        @RequestHeader(ADMIN_TOKEN_HEADER, required = false) token: String?,
        @RequestParam month: String,
    ): ResponseEntity<Map<String, Any>> {
        if (!authorized(adminToken, token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val count = asianGamesIngestService.ingestMonth(YearMonth.parse(month))
        return ResponseEntity.ok(mapOf("month" to month, "ingested" to count))
    }

    companion object {
        const val ADMIN_TOKEN_HEADER = "X-Admin-Token"

        /**
         * 순수 판정. **토큰 설정이 비어 있으면 무조건 거부한다** — 운영에서 환경변수를 빠뜨렸을 때
         * 열리는 게 아니라 닫히게. 비교는 상수 시간(MessageDigest.isEqual)으로 한다.
         */
        fun authorized(configured: String, presented: String?): Boolean {
            if (configured.isBlank() || presented.isNullOrEmpty()) return false
            return MessageDigest.isEqual(configured.toByteArray(), presented.toByteArray())
        }
    }
}
