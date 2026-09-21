package com.seungbeom.kbo.board

import com.seungbeom.kbo.league.League
import com.seungbeom.kbo.team.TeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PostService(
    private val posts: PostRepository,
    private val teams: TeamRepository,
) {
    fun validate(form: PostForm, teamId: String, authorId: Long): List<PostViolation> {
        val violations = PostRules.validate(
            form.title, form.content, teams.findByLeague(League.KBO).mapTo(HashSet()) { it.id }, teamId,
        ).toMutableList()

        /* 도배 방지는 «지금 몇 개 썼나» 를 봐야 해서 저장소가 필요하다 — 규칙 자체는 PostRules 가 갖는다 */
        val since = Instant.now().minus(PostRules.RATE_WINDOW)
        if (posts.countByAuthorIdAndCreatedAtAfter(authorId, since) >= PostRules.RATE_LIMIT) {
            violations += PostViolation.TOO_MANY
        }
        return violations
    }

    @Transactional
    fun create(teamId: String, authorId: Long, form: PostForm): Post =
        posts.save(Post(teamId = teamId, authorId = authorId, title = form.title.trim(), content = form.content.trim()))

    @Transactional
    fun update(post: Post, form: PostForm): Post {
        post.title = form.title.trim()
        post.content = form.content.trim()
        post.updatedAt = Instant.now()
        return posts.save(post)
    }

    @Transactional
    fun softDelete(post: Post) {
        post.deletedAt = Instant.now()
        posts.save(post)
    }
}
