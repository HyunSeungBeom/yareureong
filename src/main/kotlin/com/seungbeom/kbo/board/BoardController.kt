package com.seungbeom.kbo.board

import com.seungbeom.kbo.auth.AuthController.Companion.SESSION_COOKIE
import com.seungbeom.kbo.auth.SessionService
import com.seungbeom.kbo.league.League
import com.seungbeom.kbo.auth.User
import com.seungbeom.kbo.auth.UserRepository
import com.seungbeom.kbo.team.TeamRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 팀별 팬 게시판.
 *
 * **읽기는 로그인 없이, 쓰기·수정·삭제는 로그인한 본인만.** 권한 판정은 쿠키에서 꺼낸 세션으로
 * 서버가 하고([SessionService]), 화면의 `mine` 은 버튼을 보여줄지 정하는 **표시용**일 뿐이다 —
 * 프론트를 조작해도 남의 글은 고쳐지지 않는다.
 */
@RestController
@RequestMapping("/api/board")
class BoardController(
    private val posts: PostRepository,
    private val users: UserRepository,
    private val teams: TeamRepository,
    private val sessions: SessionService,
    private val postService: PostService,
) {

    @GetMapping("/{teamId}/posts")
    fun list(
        @PathVariable teamId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CookieValue(name = SESSION_COOKIE, required = false) token: String?,
    ): ResponseEntity<PostPage> {
        if (!teams.existsById(teamId)) return ResponseEntity.notFound().build()

        val me = sessions.userOf(token)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 50))
        val found = posts.findByTeamIdAndDeletedAtIsNullOrderByCreatedAtDesc(teamId, pageable)
        val authors = users.findAllById(found.content.map { it.authorId }).associateBy { it.id }

        return ResponseEntity.ok(
            PostPage(
                items = found.content.map { view(it, authors[it.authorId], me) },
                page = found.number,
                size = found.size,
                totalPages = found.totalPages,
                totalItems = found.totalElements,
            ),
        )
    }

    @GetMapping("/posts/{id}")
    fun detail(
        @PathVariable id: Long,
        @CookieValue(name = SESSION_COOKIE, required = false) token: String?,
    ): ResponseEntity<PostView> {
        val post = posts.findByIdAndDeletedAtIsNull(id) ?: return ResponseEntity.notFound().build()
        val me = sessions.userOf(token)
        return ResponseEntity.ok(view(post, users.findById(post.authorId).orElse(null), me))
    }

    @PostMapping("/{teamId}/posts")
    fun create(
        @PathVariable teamId: String,
        @RequestBody form: PostForm,
        @CookieValue(name = SESSION_COOKIE, required = false) token: String?,
    ): ResponseEntity<Any> {
        val me = sessions.userOf(token) ?: return unauthorized()
        val myId = me.id!!
        val violations = postService.validate(form, teamId, myId)
        if (violations.isNotEmpty()) return badRequest(violations)

        val saved = postService.create(teamId, myId, form)
        return ResponseEntity.status(HttpStatus.CREATED).body(view(saved, me, me))
    }

    @PutMapping("/posts/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody form: PostForm,
        @CookieValue(name = SESSION_COOKIE, required = false) token: String?,
    ): ResponseEntity<Any> {
        val me = sessions.userOf(token) ?: return unauthorized()
        val post = posts.findByIdAndDeletedAtIsNull(id) ?: return ResponseEntity.notFound().build()
        if (!PostRules.canModify(post, me.id)) return forbidden()

        val violations = PostRules.validate(form.title, form.content, teamIds(), post.teamId)
        if (violations.isNotEmpty()) return badRequest(violations)

        return ResponseEntity.ok(view(postService.update(post, form), me, me))
    }

    @DeleteMapping("/posts/{id}")
    fun delete(
        @PathVariable id: Long,
        @CookieValue(name = SESSION_COOKIE, required = false) token: String?,
    ): ResponseEntity<Any> {
        val me = sessions.userOf(token) ?: return unauthorized()
        val post = posts.findByIdAndDeletedAtIsNull(id) ?: return ResponseEntity.notFound().build()
        if (!PostRules.canModify(post, me.id)) return forbidden()

        postService.softDelete(post)
        return ResponseEntity.noContent().build()
    }

    private fun teamIds(): Set<String> = teams.findByLeague(League.KBO).mapTo(HashSet()) { it.id }

    private fun view(post: Post, author: User?, me: User?) = PostView(
        id = post.id!!,
        teamId = post.teamId,
        title = post.title,
        content = post.content,
        authorNickname = author?.nickname ?: "탈퇴한 사용자",
        authorProfileImageUrl = author?.profileImageUrl,
        createdAt = post.createdAt,
        updatedAt = post.updatedAt,
        edited = post.updatedAt.isAfter(post.createdAt.plusSeconds(1)),
        mine = me != null && post.authorId == me.id,
    )

    private fun unauthorized(): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(problem(HttpStatus.UNAUTHORIZED, "로그인이 필요해요", listOf("로그인 후 다시 시도해 주세요")))

    private fun forbidden(): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(problem(HttpStatus.FORBIDDEN, "권한이 없어요", listOf("내가 쓴 글만 고치거나 지울 수 있어요")))

    private fun badRequest(violations: List<PostViolation>): ResponseEntity<Any> = ResponseEntity.badRequest()
        .body(problem(HttpStatus.BAD_REQUEST, "글을 저장하지 못했어요", violations.map { it.message }))

    /** 실패 응답 모양은 경기 검색 API 와 같다(`errors` 목록) — 프론트가 같은 코드로 읽는다. */
    private fun problem(status: HttpStatus, detail: String, errors: List<String>): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply { setProperty("errors", errors) }
}
