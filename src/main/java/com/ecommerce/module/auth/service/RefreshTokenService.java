package com.ecommerce.module.auth.service;

public interface RefreshTokenService {
    String createRefreshToken(Long userId, String deviceInfo);

    Long validateRefreshToken(String token);

    void invalidate(String token);

    void invalidateAllForUser(Long userId);

    String rotateRefreshToken(String oldToken, Long expectedUserId, String deviceInfo);
}