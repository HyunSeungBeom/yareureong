package com.seungbeom.kbo.auth

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 소셜 프로필 → 우리 사용자. 처음이면 만들고, 있으면 닉네임·사진을 최신으로 맞춘다. */
@Service
class UserService(private val users: UserRepository) {

    @Transactional
    fun upsert(provider: AuthProvider, profile: KakaoProfile): User {
        val existing = users.findByProviderAndProviderUserId(provider.name, profile.id)
        if (existing != null) {
            existing.nickname = profile.nickname
            existing.profileImageUrl = profile.profileImageUrl
            return users.save(existing)
        }
        return users.save(
            User(
                provider = provider.name,
                providerUserId = profile.id,
                nickname = profile.nickname,
                profileImageUrl = profile.profileImageUrl,
            ),
        )
    }
}
