package org.elearning.backend.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.service.TokenBlackListService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.security.auth.CustomUserDetailsService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlackListService tokenBlacklistService;
    private static final int LENGTH_OF_BEARER_WORD = 7;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        System.out.println("[JWT] " + request.getMethod() + " " + path);
        System.out.println("[JWT] Authorization present: " + (header != null));

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(LENGTH_OF_BEARER_WORD);

            try {
                boolean revoked = tokenBlacklistService.isRevoked(token);
                System.out.println("[JWT] token blacklisted? " + revoked);

                if (revoked) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                var userId = jwtUtil.extractId(token);
                System.out.println("[JWT] extracted userId: " + userId);

                CustomUserDetails userDetails = customUserDetailsService.loadUserById(userId);
                System.out.println("[JWT] user enabled: " + userDetails.isEnabled());

                if (!userDetails.isEnabled()) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("[JWT] authentication set");
            } catch (RuntimeException e) {
                System.out.println("[JWT] authentication failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
