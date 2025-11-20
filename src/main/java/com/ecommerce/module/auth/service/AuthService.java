package com.ecommerce.module.auth.service;

import com.ecommerce.module.auth.dto.request.RegisterRequest;
import com.ecommerce.module.auth.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    void verifyEmail(String token);

    void resendVerification(String email);

    AuthResponse login(String email, String password, String deviceInfo);

    AuthResponse oauthLogin(String provider, String providerUserId, String email, String requestedRole,
            String deviceInfo);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}