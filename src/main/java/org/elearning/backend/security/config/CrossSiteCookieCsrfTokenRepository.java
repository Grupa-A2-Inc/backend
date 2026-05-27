package org.elearning.backend.security.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import java.util.UUID;

final class CrossSiteCookieCsrfTokenRepository implements CsrfTokenRepository {

    static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";
    static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";
    static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(
                DEFAULT_CSRF_HEADER_NAME,
                DEFAULT_CSRF_PARAMETER_NAME,
                UUID.randomUUID().toString()
        );
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = token != null ? token.getToken() : "";

        ResponseCookie cookie = ResponseCookie.from(DEFAULT_CSRF_COOKIE_NAME, tokenValue)
                .httpOnly(false)
                .secure(request.isSecure())
                .sameSite("None")
                .path("/")
                .maxAge(token != null ? -1 : 0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, DEFAULT_CSRF_COOKIE_NAME);
        if (cookie == null || !StringUtils.hasText(cookie.getValue())) {
            return null;
        }

        return new DefaultCsrfToken(
                DEFAULT_CSRF_HEADER_NAME,
                DEFAULT_CSRF_PARAMETER_NAME,
                cookie.getValue()
        );
    }
}
