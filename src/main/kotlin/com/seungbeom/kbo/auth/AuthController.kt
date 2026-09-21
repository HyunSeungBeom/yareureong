package com.seungbeom.kbo.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 카카오 로그인 — 브라우저는 토큰을 만지지 않는다.
 *
 * 1. `/api/auth/kakao/login` → 카카오 동의 화면으로 보낸다(state 쿠키 심음 = CSRF 방지)
 * 2. 카카오가 `/api/auth/kakao/callback?code=&state=` 로 돌려보낸다
 * 3. 서버가 코드를 토큰으로 바꾸고 프로필을 받아 사용자 upsert → **세션 쿠키(HttpOnly)** 발급
 * 4. 원래 보던 페이지로 돌려보낸다
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val kakao: KakaoClient,
    private val sessions: SessionService,
    private val userService: UserService,
    @Value("\${app.auth.web-base-url:http://localhost:3000}") private val webBaseUrl: String,
    @Value("\${app.auth.cookie-secure:true}") private val cookieSecure: Boolean,
) {

    @GetMapping("/kakao/login")
    fun login(
        @RequestParam(required = false) redirect: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        if (!kakao.configured) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

        val state = SessionTokens.newToken()
        response.addHeader("Set-Cookie", stateCookie(state).toString())
        response.addHeader("Set-Cookie", returnToCookie(safeRedirect(redirect)).toString())
        return ResponseEntity.status(HttpStatus.FOUND)
            .header("Location", kakao.authorizeUrl(state))
            .build()
    }

    @GetMapping("/kakao/callback")
    fun callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @CookieValue(name = STATE_COOKIE, required = false) expectedState: String?,
        @CookieValue(name = RETURN_COOKIE, required = false) returnTo: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        /* state 가 다르면 남이 시작시킨 로그인이다(CSRF) — 코드가 유효해도 받지 않는다 */
        if (code.isNullOrBlank() || state.isNullOrBlank() || state != expectedState) {
            return redirectTo(returnTo, "login_failed", response)
        }
        val profile = kakao.profileOf(code) ?: return redirectTo(returnTo, "login_failed", response)

        val user = userService.upsert(AuthProvider.KAKAO, profile)
        response.addHeader("Set-Cookie", sessionCookie(sessions.issue(user.id!!)).toString())
        return redirectTo(returnTo, null, response)
    }

    @GetMapping("/me")
    fun me(@CookieValue(name = SESSION_COOKIE, required = false) token: String?): ResponseEntity<Me> {
        val user = sessions.userOf(token) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(Me(user.id!!, user.nickname, user.profileImageUrl))
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = SESSION_COOKIE, required = false) token: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        sessions.revoke(token)
        response.addHeader("Set-Cookie", expiredCookie(SESSION_COOKIE).toString())
        return ResponseEntity.noContent().build()
    }

    /* ── 쿠키 ─────────────────────────────────────────── */

    private fun sessionCookie(token: String) = baseCookie(SESSION_COOKIE, token)
        .httpOnly(true)               // JS 가 못 읽는다 — XSS 가 나도 세션을 못 훔친다
        .maxAge(SessionService.LIFETIME)
        .build()

    private fun stateCookie(state: String) = baseCookie(STATE_COOKIE, state)
        .httpOnly(true).maxAge(java.time.Duration.ofMinutes(10)).build()

    private fun returnToCookie(path: String) = baseCookie(RETURN_COOKIE, path)
        .httpOnly(true).maxAge(java.time.Duration.ofMinutes(10)).build()

    private fun expiredCookie(name: String) = baseCookie(name, "").maxAge(0).httpOnly(true).build()

    private fun baseCookie(name: String, value: String) = ResponseCookie.from(name, value)
        .path("/")
        .secure(cookieSecure)
        .sameSite("Lax")              // 프론트와 API 가 같은 사이트라 Lax 로 충분하다
        .httpOnly(true)

    private fun redirectTo(returnTo: String?, error: String?, response: HttpServletResponse): ResponseEntity<Void> {
        response.addHeader("Set-Cookie", expiredCookie(STATE_COOKIE).toString())
        response.addHeader("Set-Cookie", expiredCookie(RETURN_COOKIE).toString())
        val path = safeRedirect(returnTo)
        val suffix = error?.let { if (path.contains("?")) "&error=$it" else "?error=$it" } ?: ""
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", "$webBaseUrl$path$suffix").build()
    }

    companion object {
        const val SESSION_COOKIE = "kbo_session"
        const val STATE_COOKIE = "kbo_oauth_state"
        const val RETURN_COOKIE = "kbo_return_to"

        /**
         * 돌아갈 주소는 **우리 사이트 안의 경로만** 허용한다. 그대로 믿으면 로그인 링크로 남을
         * 피싱 사이트에 보낼 수 있다(open redirect).
         */
        fun safeRedirect(raw: String?): String =
            raw?.takeIf { it.startsWith("/") && !it.startsWith("//") } ?: "/"
    }
}
