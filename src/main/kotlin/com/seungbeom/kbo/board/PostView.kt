package com.seungbeom.kbo.board

import java.time.Instant

/** 화면에 내려보내는 글 한 건. `mine` 은 «지금 보는 사람이 글쓴이인가» — 수정·삭제 버튼 노출 판단. */
data class PostView(
    val id: Long,
    val teamId: String,
    val title: String,
    val content: String,
    val authorNickname: String,
    val authorProfileImageUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val edited: Boolean,
    val mine: Boolean,
)

data class PostPage(val items: List<PostView>, val page: Int, val size: Int, val totalPages: Int, val totalItems: Long)

data class PostForm(val title: String = "", val content: String = "")
