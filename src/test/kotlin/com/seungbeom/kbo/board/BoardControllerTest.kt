package com.seungbeom.kbo.board

import com.seungbeom.kbo.auth.AuthController.Companion.SESSION_COOKIE
import com.seungbeom.kbo.auth.SessionService
import com.seungbeom.kbo.auth.User
import com.seungbeom.kbo.auth.UserRepository
import com.seungbeom.kbo.team.Team
import com.seungbeom.kbo.team.TeamRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.data.domain.PageImpl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import com.seungbeom.kbo.league.League

/**
 * 게시판 권한 — 여기가 뚫리면 **남이 내 글을 고치거나 지운다.**
 * 서버가 쿠키만 보고 판단하는지(프론트 값이나 요청 본문을 믿지 않는지) 확인한다.
 */
@WebMvcTest(BoardController::class)
class BoardControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @MockitoBean lateinit var posts: PostRepository
    @MockitoBean lateinit var users: UserRepository
    @MockitoBean lateinit var teams: TeamRepository
    @MockitoBean lateinit var sessions: SessionService
    @MockitoBean lateinit var postService: PostService

    /** Mockito 의 any() 는 null 이라 Kotlin 의 non-null 파라미터와 안 맞는다 — 그 자리를 메운다 */
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = ArgumentMatchers.any<T>() as T

    private val author = User(id = 7, providerUserId = "kakao-7", nickname = "두산팬")
    private val stranger = User(id = 8, providerUserId = "kakao-8", nickname = "남")
    private val post = Post(id = 1, teamId = "OB", authorId = 7, title = "제목", content = "내용")

    private fun loggedInAs(user: User?, token: String = "tok") {
        given(sessions.userOf(token)).willReturn(user)
    }

    private fun body(title: String = "제목", content: String = "내용") =
        """{"title":"$title","content":"$content"}"""

    @Test
    fun `로그인하지 않으면 글을 쓸 수 없다`() {
        mvc.post("/api/board/OB/posts") {
            contentType = MediaType.APPLICATION_JSON
            content = body()
        }.andExpect { status { isUnauthorized() } }

        verify(postService, never()).create(anyString(), anyLong(), anyArg())
    }

    @Test
    fun `남의 글은 고칠 수 없다 — 로그인해도 글쓴이가 아니면 403`() {
        loggedInAs(stranger)
        given(posts.findByIdAndDeletedAtIsNull(1)).willReturn(post)

        mvc.put("/api/board/posts/1") {
            cookie(Cookie(SESSION_COOKIE, "tok"))
            contentType = MediaType.APPLICATION_JSON
            content = body(title = "바꿔치기")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.errors[0]") { value("내가 쓴 글만 고치거나 지울 수 있어요") }
        }

        verify(postService, never()).update(anyArg(), anyArg())
    }

    @Test
    fun `남의 글은 지울 수 없다`() {
        loggedInAs(stranger)
        given(posts.findByIdAndDeletedAtIsNull(1)).willReturn(post)

        mvc.delete("/api/board/posts/1") {
            cookie(Cookie(SESSION_COOKIE, "tok"))
        }.andExpect { status { isForbidden() } }

        verify(postService, never()).softDelete(anyArg())
    }

    @Test
    fun `내 글은 고칠 수 있다`() {
        loggedInAs(author)
        given(posts.findByIdAndDeletedAtIsNull(1)).willReturn(post)
        given(teams.findByLeague(League.KBO)).willReturn(listOf(Team("OB", "두산 베어스")))
        given(postService.update(anyArg(), anyArg())).willReturn(post)

        mvc.put("/api/board/posts/1") {
            cookie(Cookie(SESSION_COOKIE, "tok"))
            contentType = MediaType.APPLICATION_JSON
            content = body(title = "고친 제목")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `규칙을 어기면 400 과 사유를 돌려준다`() {
        loggedInAs(author)
        given(postService.validate(anyArg(), anyString(), anyLong()))
            .willReturn(listOf(PostViolation.EMPTY_TITLE, PostViolation.TOO_MANY))

        mvc.post("/api/board/OB/posts") {
            cookie(Cookie(SESSION_COOKIE, "tok"))
            contentType = MediaType.APPLICATION_JSON
            content = body(title = "")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0]") { value("제목을 입력해 주세요") }
            jsonPath("$.errors[1]") { value("잠시 후에 다시 써주세요 (1분에 3개까지)") }
        }
    }

    @Test
    fun `목록은 로그인 없이도 볼 수 있다 — 읽기는 누구나`() {
        given(teams.existsById("OB")).willReturn(true)
        given(posts.findByTeamIdAndDeletedAtIsNullOrderByCreatedAtDesc(anyString(), anyArg()))
            .willReturn(PageImpl(listOf(post)))
        given(users.findAllById(anyArg())).willReturn(listOf(author))

        mvc.get("/api/board/OB/posts").andExpect {
            status { isOk() }
            jsonPath("$.items[0].title") { value("제목") }
            jsonPath("$.items[0].authorNickname") { value("두산팬") }
            /* 로그인하지 않았으니 수정·삭제 버튼을 띄울 근거가 없다 */
            jsonPath("$.items[0].mine") { value(false) }
        }
    }

    @Test
    fun `없는 게시판은 404`() {
        given(teams.existsById("XX")).willReturn(false)

        mvc.get("/api/board/XX/posts").andExpect { status { isNotFound() } }
    }
}
