package com.seungbeom.kbo.auth

/** 화면에 내려보내는 «나» — 내부 id 는 숨기지 않아도 되지만 필요한 것만 준다. */
data class Me(val id: Long, val nickname: String, val profileImageUrl: String?)
