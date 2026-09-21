package com.seungbeom.kbo.ingest

import com.seungbeom.kbo.ingest.IngestController.Companion.ADMIN_TOKEN_HEADER
import com.seungbeom.kbo.ingest.IngestController.Companion.authorized
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.YearMonth
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IngestControllerTest {

    /** 판정 규칙 — Spring 없이 */
    @Nested
    inner class Authorization {
        @Test
        fun `설정과 같은 토큰만 통과한다`() {
            assertTrue(authorized("s3cret", "s3cret"))
            assertFalse(authorized("s3cret", "wrong"))
            assertFalse(authorized("s3cret", "s3cret-longer"))
            assertFalse(authorized("s3cret", null))
        }

        @Test
        fun `설정이 비어 있으면 무엇을 보내도 거부한다 — 환경변수를 빠뜨려도 열리지 않게`() {
            assertFalse(authorized("", ""))
            assertFalse(authorized("", "anything"))
            assertFalse(authorized("   ", "   "))
        }
    }

    /** 엔드포인트 배선 — 헤더 이름·응답 코드·서비스 호출 여부 */
    @WebMvcTest(IngestController::class, properties = ["app.admin.token=s3cret"])
    class Endpoint {
        @Autowired lateinit var mvc: MockMvc
        @MockitoBean lateinit var ingestService: ScheduleIngestService

        @MockitoBean lateinit var asianGamesIngestService: AsianGamesIngestService

        @Test
        fun `토큰이 없으면 403 이고 수집하지 않는다`() {
            mvc.post("/api/admin/ingest?month=2026-08").andExpect { status { isForbidden() } }
            verifyNoInteractions(ingestService)
        }

        @Test
        fun `토큰이 틀리면 403 이고 수집하지 않는다`() {
            mvc.post("/api/admin/ingest?month=2026-08") { header(ADMIN_TOKEN_HEADER, "wrong") }
                .andExpect { status { isForbidden() } }
            verifyNoInteractions(ingestService)
        }

        @Test
        fun `토큰이 맞으면 그 달을 수집하고 건수를 돌려준다`() {
            given(ingestService.ingestMonth(YearMonth.of(2026, 8))).willReturn(86)

            mvc.post("/api/admin/ingest?month=2026-08") { header(ADMIN_TOKEN_HEADER, "s3cret") }
                .andExpect {
                    status { isOk() }
                    jsonPath("$.ingested") { value(86) }
                }
            verify(ingestService).ingestMonth(YearMonth.of(2026, 8))
        }

        @Test
        fun `아시안게임 수집도 같은 토큰으로 막힌다`() {
            // 출처가 다르다고 보호가 빠지면 공개 주소로 스크래핑을 반복 실행시킬 수 있다.
            mvc.post("/api/admin/ingest/asian-games?month=2026-09")
                .andExpect { status { isForbidden() } }
            verifyNoInteractions(asianGamesIngestService)
        }

        @Test
        fun `아시안게임 수집은 KBO 수집을 건드리지 않는다`() {
            given(asianGamesIngestService.ingestMonth(YearMonth.of(2026, 9))).willReturn(22)

            mvc.post("/api/admin/ingest/asian-games?month=2026-09") { header(ADMIN_TOKEN_HEADER, "s3cret") }
                .andExpect {
                    status { isOk() }
                    jsonPath("$.ingested") { value(22) }
                }
            verifyNoInteractions(ingestService)
        }
    }
}
