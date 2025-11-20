package com.ecommerce.module.auth.service.impl;

import com.ecommerce.module.auth.dto.request.RegisterRequest;
import com.ecommerce.module.auth.dto.response.AuthResponse;
import com.ecommerce.module.auth.entity.AuthOauthAccount;
import com.ecommerce.module.auth.exception.BadRequestException;
import com.ecommerce.module.auth.exception.ConflictException;
import com.ecommerce.module.auth.exception.NotFoundException;
import com.ecommerce.module.auth.exception.UnauthorizedException;
import com.ecommerce.module.auth.repository.AuthOauthAccountRepository;
import com.ecommerce.module.auth.service.AuthService;
import com.ecommerce.module.auth.service.EmailService;
import com.ecommerce.module.auth.service.RefreshTokenService;
import com.ecommerce.module.user.entity.User;
import com.ecommerce.module.user.repository.UserRepository;
import com.ecommerce.module.auth.util.RedisHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    public final UserRepository userRepository;
    private final AuthOauthAccountRepository oauthRepo;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final RedisHelper redisHelper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    private static final long EMAIL_VERIFY_TTL = 60 * 60 * 24; // 24h
    private static final long PW_RESET_TTL = 30 * 60; // 30m

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("DUPLICATE_ERROR", "Email already registered");
        }
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isEmailVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        redisHelper.set("email_verify:" + token, user.getEmail(), EMAIL_VERIFY_TTL);
        String link = buildVerifyLink(token);
        try {
            emailService.sendVerificationEmail(user.getEmail(), link);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", user.getEmail(), e);
            throw new RuntimeException("INTERNAL_ERROR");
        }
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole(), user.getFullName(), null);
    }

    @Override
    public void verifyEmail(String token) {
        String key = "email_verify:" + token;
        String email = redisHelper.get(key);
        if (email == null)
            throw new UnauthorizedException("INVALID_TOKEN", "Token invalid or expired");
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty())
            throw new NotFoundException("USER_NOT_FOUND");
        User user = opt.get();
        user.setIsEmailVerified(true);
        userRepository.save(user);
        redisHelper.delete(key);
    }

    @Override
    public void resendVerification(String email) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty())
            return; // don't reveal
        User user = opt.get();
        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new BadRequestException("ALREADY_VERIFIED", "Email already verified");
        }
        String token = UUID.randomUUID().toString();
        redisHelper.set("email_verify:" + token, email, EMAIL_VERIFY_TTL);
        String link = buildVerifyLink(token);
        try {
            emailService.sendVerificationEmail(email, link);
        } catch (MessagingException e) {
            log.error("Failed to resend verification email to {}", email, e);
            throw new RuntimeException("INTERNAL_ERROR");
        }
    }

    @Override
    public AuthResponse login(String email, String password, String deviceInfo) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty())
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials");
        User user = opt.get();
        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials");
        if (!Boolean.TRUE.equals(user.getIsEmailVerified()))
            throw new UnauthorizedException("EMAIL_NOT_VERIFIED", "Email not verified");

        String refresh = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole(), user.getFullName(), refresh);
    }

    @Override
    public AuthResponse oauthLogin(String provider, String providerUserId, String email, String requestedRole,
            String deviceInfo) {
        Optional<AuthOauthAccount> map = oauthRepo.findByProviderAndProviderUserId(provider, providerUserId);
        if (map.isPresent()) {
            Long uid = map.get().getUserId();
            User user = userRepository.findById(uid).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));
            String refresh = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);
            return new AuthResponse(user.getId(), user.getEmail(), user.getRole(), user.getFullName(), refresh);
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            AuthOauthAccount a = AuthOauthAccount.builder().userId(user.getId()).provider(provider)
                    .providerUserId(providerUserId).email(email).build();
            oauthRepo.save(a);
            String refresh = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);
            return new AuthResponse(user.getId(), user.getEmail(), user.getRole(), user.getFullName(), refresh);
        }

        User user = User.builder().email(email).fullName("").role(requestedRole == null ? "CUSTOMER" : requestedRole)
                .isEmailVerified(true).isActive(true).build();
        user = userRepository.save(user);
        AuthOauthAccount a = AuthOauthAccount.builder().userId(user.getId()).provider(provider)
                .providerUserId(providerUserId).email(email).build();
        oauthRepo.save(a);
        String refresh = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole(), user.getFullName(), refresh);
    }

    @Override
    public void forgotPassword(String email) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty())
            return; // don't reveal
        User user = opt.get();
        String token = UUID.randomUUID().toString();
        redisHelper.set("password_reset:" + token, user.getEmail(), PW_RESET_TTL);
        String link = buildResetLink(token);
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), link);
        } catch (MessagingException e) {
            log.error("Failed to send reset email to {}", email, e);
            throw new RuntimeException("INTERNAL_ERROR");
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String key = "password_reset:" + token;
        String email = redisHelper.get(key);
        if (email == null)
            throw new UnauthorizedException("INVALID_TOKEN", "Token invalid or expired");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.invalidateAllForUser(user.getId());
        redisHelper.delete(key);
    }

    private String buildVerifyLink(String token) {
        return "https://app.example.com/auth/verify-email?token=" + token;
    }

    private String buildResetLink(String token) {
        return "https://app.example.com/auth/reset-password?token=" + token;
    }
}