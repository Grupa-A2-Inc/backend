package org.elearning.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.request.ForgotPasswordRequest;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.request.ResetPasswordRequest;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.response.ResetPasswordResponse;
import org.elearning.backend.auth.service.AuthService;
import org.elearning.backend.auth.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;


@RestController
@Tag(name = "Authentication", description = "Endpoints for authentication and session management")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService resetService;

    @Value("${app.auth.secure-cookies:true}")
    private boolean secureCookies;

    @Operation(
            summary = "Register a new account",
            description = "Registers a new organization account and returns the authentication response. A refresh token is also sent as an HttpOnly cookie," +
                    "the refresh token field inside the body will always be null."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registration successful",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content
    )
    @ApiResponse(
            responseCode = "409",
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
            description = "Authenticates a user with email and password, returns the authentication response, and sends a refresh token as an HttpOnly cookie," +
                    "the refresh token field inside the body will always be null."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return responseWithRefreshCookie(response);
    }

    @Operation(
            summary = "Request password reset",
            description = "Accepts an email address and always returns a generic response message. If the account exists, a reset token is generated and sent by email."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Password reset request processed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResetPasswordResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ResetPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        ResetPasswordResponse response = resetService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reset password",
            description = "Consumes a password reset token and updates the user's password if the token is valid and not expired."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Password reset processed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ResetPasswordResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or reset token",
            content = @Content
    )
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        ResetPasswordResponse response = resetService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Logout user",
            description = "Clears the refresh token cookie by returning the same cookie with an empty value and Max-Age=0."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Logout successful",
            content = @Content
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expiredCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("None")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    private ResponseEntity<AuthResponse> responseWithRefreshCookie(AuthResponse response) {
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("None")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.setRefreshToken(null);

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }
}
