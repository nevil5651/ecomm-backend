package com.ecommerce.module.auth.service.impl;

import com.ecommerce.module.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration TTL = Duration.ofDays(7);
    private static final String KEY_PREFIX = "auth:refresh:";
    private static final String USER_SET_PREFIX = "auth:user-refreshs:";

    @Override
    public String createRefreshToken(Long userId, String deviceInfo) {
        String token = UUID.randomUUID().toString();
        String key = KEY_PREFIX + token;
        String value = userId + "|" + (deviceInfo == null ? "" : deviceInfo) + "|" + Instant.now().getEpochSecond();
        redisTemplate.opsForValue().set(key, value, TTL);
        redisTemplate.opsForSet().add(USER_SET_PREFIX + userId, token);
        return token;
    }

    @Override
    public Long validateRefreshToken(String token) {
        String key = KEY_PREFIX + token;
        String v = redisTemplate.opsForValue().get(key);
        if (v == null)
            return null;
        try {
            return Long.valueOf(v.split("\\|", 3)[0]);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void invalidate(String token) {
        String key = KEY_PREFIX + token;
        String v = redisTemplate.opsForValue().get(key);
        if (v != null) {
            String uid = v.split("\\|", 3)[0];
            redisTemplate.opsForSet().remove(USER_SET_PREFIX + uid, token);
        }
        redisTemplate.delete(key);
    }

    @Override
    public void invalidateAllForUser(Long userId) {
        Set<String> tokens = redisTemplate.opsForSet().members(USER_SET_PREFIX + userId);
        if (tokens != null) {
            for (String t : tokens)
                redisTemplate.delete(KEY_PREFIX + t);
        }
        redisTemplate.delete(USER_SET_PREFIX + userId);
    }

    @Override
    public String rotateRefreshToken(String oldToken, Long expectedUserId, String deviceInfo) {
        String oldKey = KEY_PREFIX + oldToken;
        String v = redisTemplate.opsForValue().get(oldKey);
        if (v == null)
            return null;
        String[] parts = v.split("\\|", 3);
        Long uid = Long.valueOf(parts[0]);
        if (!uid.equals(expectedUserId)) {
            invalidate(oldToken);
            return null;
        }

        String newToken = UUID.randomUUID().toString();
        String newKey = KEY_PREFIX + newToken;
        String newVal = uid + "|" + (deviceInfo == null ? "" : deviceInfo) + "|" + Instant.now().getEpochSecond();

        // Use MULTI/EXEC
        var conn = redisTemplate.getConnectionFactory().getConnection();
        try {
            conn.multi();
            redisTemplate.opsForValue().set(newKey, newVal, TTL);
            redisTemplate.opsForSet().add(USER_SET_PREFIX + uid, newToken);
            redisTemplate.delete(oldKey);
            conn.exec();
        } finally {
            conn.close();
        }
        redisTemplate.opsForSet().remove(USER_SET_PREFIX + uid, oldToken);
        return newToken;
    }
}