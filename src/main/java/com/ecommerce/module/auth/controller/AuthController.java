package com.ecommerce.module.auth.controller;

import com.ecommerce.module.auth.dto.request.RegisterRequest;
import com.ecommerce.module.auth.dto.response.AuthResponse;
import com.ecommerce.module.auth.security.JwtTokenProvider;
import com.ecommerce.module.auth.service.AuthService;
import com.ecommerce.module.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import com.ecommerce.module.user.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse resp = authService.register(request);
        return ResponseEntity.status(201).body(java.util.Map.of("status", "success", "data", java.util.Map.of(
                "userId", resp.getUserId(), "email", resp.getEmail(), "role", resp.getRole(), "isEmailVerified",
                false)));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity
                .ok(java.util.Map.of("status", "success", "data", java.util.Map.of("message", "Email verified")));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resend(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        authService.resendVerification(email);
        return ResponseEntity.ok(
                java.util.Map.of("status", "success", "data", java.util.Map.of("message", "VERIFICATION_EMAIL_SENT")));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody java.util.Map<String, String> body, HttpServletResponse response) {
        String email = body.get("email");
        String password = body.get("password");
        String deviceInfo = body.get("deviceInfo");
        AuthResponse r = authService.login(email, password, deviceInfo);

        String accessToken = jwtTokenProvider.createAccessToken(r.getUserId(), r.getEmail(),
                java.util.List.of(r.getRole()));
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(24 * 3600).build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.ok(java.util.Map.of("status", "success", "data", java.util.Map.of(
                "refreshToken", r.getRefreshToken(),
                "user", java.util.Map.of("userId", r.getUserId(), "email", r.getEmail(), "role", r.getRole()))));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refresh(@RequestBody java.util.Map<String, String> body, HttpServletResponse response) {
        String oldRefresh = body.get("refreshToken");
        Long uid = refreshTokenService.validateRefreshToken(oldRefresh);
        if (uid == null)
            return ResponseEntity.status(401).body(java.util.Map.of("status", "error", "error",
                    java.util.Map.of("code", "INVALID_TOKEN", "message", "Invalid token")));
        String newRefresh = refreshTokenService.rotateRefreshToken(oldRefresh, uid, "web");
        if (newRefresh == null)
            return ResponseEntity.status(401).body(java.util.Map.of("status", "error", "error",
                    java.util.Map.of("code", "REPLAY_DETECTED", "message", "Replay detected")));

        // load user
        var user = userRepository.findById(uid).orElse(null);
        if (user == null)
            return ResponseEntity.status(401).body(java.util.Map.of("status", "error", "error",
                    java.util.Map.of("code", "USER_NOT_FOUND", "message", "User not found")));

        String newAccess = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(),
                java.util.List.of(user.getRole()));
        ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccess).httpOnly(true).secure(true)
                .sameSite("Strict").path("/").maxAge(24 * 3600).build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity
                .ok(java.util.Map.of("status", "success", "data", java.util.Map.of("refreshToken", newRefresh)));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) java.util.Map<String, String> body,
            HttpServletResponse response) {
        String refreshToken = body == null ? null : body.get("refreshToken");
        if (refreshToken != null)
            refreshTokenService.invalidate(refreshToken);
        ResponseCookie clearAccess = ResponseCookie.from("access_token", "").httpOnly(true).secure(true).path("/")
                .maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        authService.forgotPassword(email);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "data",
                java.util.Map.of("message", "If an account exists, reset link sent")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody java.util.Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "data",
                java.util.Map.of("message", "Password reset successful")));
    }
}
