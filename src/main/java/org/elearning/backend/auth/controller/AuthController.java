package org.elearning.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.request.*;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.response.CsrfResponse;
import org.elearning.backend.auth.dto.response.RefreshResponse;
import org.elearning.backend.auth.dto.response.ResetPasswordResponse;
import org.elearning.backend.auth.exception.InvalidCredentialsException;
import org.elearning.backend.auth.service.*;
import org.elearning.backend.security.config.CsrfTokenAttributes;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;


@RestController
@Tag(
        name = "Authentication",
        description = "Endpoints for account registration, sign-in, token rotation, logout, account activation, and password recovery. " +
                "These endpoints are generally role-agnostic because they are used before or during authentication, but the returned access token " +
                "will later determine whether the caller acts as a platform-wide ADMIN, an organization-scoped ORGANIZATION_ADMIN, or another role."
)
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String OK = "200";
    private static final String NO_CONTENT = "204";
    private static final String BAD_REQUEST = "400";
    private static final String UNAUTHORIZED = "401";
    private static final String CONFLICT = "409";

    private final AuthService authService;
    private final PasswordResetService resetService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlackListService tokenBlacklistService;
    private final JwtUtil jwtUtil;

    @Value("${app.auth.api-path:/api/v1/auth}")
    private String apiAuthPath = "/api/v1/auth";

    private static final String REFRESH_STRING_LITERAL = "refresh_token";

    @Value("${app.auth.secure-cookies:true}")
    private boolean secureCookies = true;

    private final AccountActivationService accountActivationService;

    @Operation(
            summary = "Register a new account",
            description = "Creates a new account through the public registration flow and immediately starts an authenticated session for the new user. " +
                    "The endpoint returns the authentication payload in the response body and also sends a refresh token as an HttpOnly cookie. " +
                    "It also initializes the CSRF flow by causing the backend to issue an XSRF-TOKEN cookie that browser clients must echo in the X-XSRF-TOKEN header " +
                    "when later calling cookie-authenticated state-changing endpoints such as refresh and logout. " +
                    "For security reasons, the refresh token value is not exposed in the JSON body and the corresponding field in the response payload " +
                    "will always be null. This endpoint is not meant for privileged staff provisioning; accounts created by ADMIN or ORGANIZATION_ADMIN " +
                    "through internal management flows should use the user-management endpoints instead."
    )
    @ApiResponse(
            responseCode = OK,
            description = "Registration successful",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthResponse.class)
            )
    )
    @ApiResponse(
            responseCode = BAD_REQUEST,
            description = "Invalid request data",
            content = @Content
    )
    @ApiResponse(
            responseCode = CONFLICT,
            description = "Conflict - account already exists or data violates business rules",
            content = @Content
    )
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return responseWithRefreshCookie(response);
    }

    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with email and password, returns a fresh access token in the response body, and sends a refresh token " +
                    "as an HttpOnly cookie. The refresh token field inside the JSON response is intentionally cleared and will always be null. " +
                    "It also initializes the CSRF flow by causing the backend to issue an XSRF-TOKEN cookie that browser clients must echo in the X-XSRF-TOKEN header " +
                    "when later calling cookie-authenticated state-changing endpoints such as refresh and logout. " +
                    "This endpoint is shared by every role in the system, including ADMIN and ORGANIZATION_ADMIN. The distinction between those roles " +
                    "does not affect login itself; it affects which protected endpoints can be used after authentication succeeds."
    )
    @ApiResponse(
            responseCode = OK,
            description = "Login successful",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthResponse.class)
            )
    )
    @ApiResponse(
            responseCode = BAD_REQUEST,
            description = "Invalid request data",
            content = @Content
    )
    @ApiResponse(
            responseCode = UNAUTHORIZED,
            description = "Invalid credentials",
            content = @Content
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return responseWithRefreshCookie(response);
    }

    @Operation(
            summary = "Get CSRF token",
            description = "Returns a CORS-readable copy of the CSRF token and its request header name. " +
                    "Browser clients on a different origin should call this endpoint with credentials included, " +
                    "store the returned token in memory, and send it in the X-XSRF-TOKEN header when calling " +
                    "cookie-authenticated endpoints such as refresh and logout."
    )
    @ApiResponse(
            responseCode = OK,
            description = "CSRF token issued",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CsrfResponse.class)
            )
    )
    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(
            @Parameter(hidden = true)
            @RequestAttribute(CsrfTokenAttributes.RAW_CSRF_TOKEN) CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName()));
    }

    @Operation(
            summary = "Activate account and set password",
            description = "Consumes an activation token generated when an admin creates a user account, " +
                    "sets the initial password, and marks the account as active. " +
                    "This endpoint is separate from the forgot-password flow. In practice, it is commonly used after an ADMIN or an ORGANIZATION_ADMIN " +
                    "creates a managed account for someone else and the invited user needs to complete first-time access securely."
    )
    @ApiResponse(responseCode = OK, description = "Account activated and password set successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResetPasswordResponse.class)))
    @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid or expired token, or passwords do not match", content = @Content)
    @PostMapping("/set-password")
    public ResponseEntity<ResetPasswordResponse> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        ResetPasswordResponse response = accountActivationService.setPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Request password reset",
            description = "Accepts an email address and always returns a generic response message, regardless of whether the address exists in the system. " +
                    "If a matching account is found, a password reset token is generated and sent by email. This behavior helps avoid user-enumeration leaks. " +
                    "The endpoint is available to every account type and is independent from ADMIN or ORGANIZATION_ADMIN privileges."
    )
    @ApiResponse(
            responseCode = OK,
            description = "Password reset request processed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResetPasswordResponse.class)
            )
    )
    @ApiResponse(
            responseCode = BAD_REQUEST,
            description = "Invalid request data",
            content = @Content
    )
    @PostMapping("/password-reset/request")
    public ResponseEntity<ResetPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        ResetPasswordResponse response = resetService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reset password",
            description = "Consumes a password reset token and replaces the user's password if the token is valid, belongs to the intended account, " +
                    "and has not expired. This endpoint is part of the self-service recovery flow and does not rely on ADMIN or ORGANIZATION_ADMIN privileges."
    )
    @ApiResponse(
            responseCode = OK,
            description = "Password reset processed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResetPasswordResponse.class)
            )
    )
    @ApiResponse(
            responseCode = BAD_REQUEST,
            description = "Invalid request data or reset token",
            content = @Content
    )
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        ResetPasswordResponse response = resetService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Validates the refresh token received through the HttpOnly cookie, rotates that refresh token, and issues a new access token. " +
                    "The caller receives the new access token in the response body and a replacement refresh token cookie in the response headers. " +
                    "Because this endpoint authenticates through a browser cookie, it is also protected by CSRF: the client must send the XSRF-TOKEN cookie value " +
                    "back in the X-XSRF-TOKEN request header and include credentials/cookies with the request. " +
                    "This endpoint keeps existing role identity intact: if the user was authenticated as ADMIN or ORGANIZATION_ADMIN, the new access token " +
                    "will preserve that role and its authorization scope."
    )
    @ApiResponse(responseCode = OK, description = "New access token issued and refresh token cookie sent",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RefreshResponse.class)))
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token", content = @Content)
    @ApiResponse(responseCode = UNAUTHORIZED, description = "Invalid, expired or revoked refresh token", content = @Content)
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @Parameter(
                    description = "Refresh token sent automatically by the browser as an HttpOnly cookie.",
                    example = "refresh_token=<http-only-cookie-value>"
            )
            @CookieValue(name = "refresh_token", required = false) String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException("Refresh token missing");
        }

        User user = refreshTokenService.getUserFromToken(rawRefreshToken);
        String newRawRefreshToken = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().getName());

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_STRING_LITERAL, newRawRefreshToken)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("None")
                .path(apiAuthPath)
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new RefreshResponse(newAccessToken));
    }

    @Operation(
            summary = "Logout user",
            description = "Clears the refresh token cookie by returning the same cookie with an empty value and Max-Age=0." +
                    " Aditionally adds the current access token to a blacklist until expiration. " +
                    "Because the endpoint can act on cookie-authenticated session state, browser clients must include credentials/cookies and must echo the XSRF-TOKEN cookie " +
                    "in the X-XSRF-TOKEN request header. " +
                    "This endpoint does not distinguish between ADMIN, ORGANIZATION_ADMIN, or any other authenticated role; " +
                    "it simply terminates the current session material as safely as possible."
    )
    @ApiResponse(responseCode = NO_CONTENT, description = "Logout successful", content = @Content)
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token", content = @Content)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(
                    description = "Refresh token sent automatically by the browser as an HttpOnly cookie.",
                    example = "refresh_token=<http-only-cookie-value>"
            )
            @CookieValue(name = "refresh_token", required = false) String rawRefreshToken,
            @Parameter(
                    description = "Bearer access token to blacklist on logout. Optional, but recommended when the caller also wants the current access token revoked.",
                    example = "Bearer eyJhbGciOiJIUzI1NiJ9..."
            )
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeForToken(rawRefreshToken);
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String rawAccessToken = authHeader.substring(7);
            try {
                LocalDateTime expiresAt = jwtUtil.extractExpiration(rawAccessToken);
                tokenBlacklistService.revokeAccessToken(rawAccessToken, expiresAt);
            } catch (Exception ignored) {
                //Exceptions ignored
            }
        }

        ResponseCookie expiredCookie = ResponseCookie.from(REFRESH_STRING_LITERAL, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("None")
                .path(apiAuthPath)
                .maxAge(0)
                .build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    private ResponseEntity<AuthResponse> responseWithRefreshCookie(AuthResponse response) {
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_STRING_LITERAL, response.getRefreshToken())
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("None")
                .path(apiAuthPath)
                .maxAge(Duration.ofDays(7))
                .build();

        response.setRefreshToken(null);

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }
}
