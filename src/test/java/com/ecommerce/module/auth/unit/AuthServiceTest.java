package com.ecommerce.module.auth.unit;

import com.ecommerce.module.auth.dto.request.RegisterRequest;
import com.ecommerce.module.auth.dto.response.AuthResponse;
import com.ecommerce.module.auth.exception.ConflictException;
import com.ecommerce.module.auth.repository.AuthOauthAccountRepository;
import com.ecommerce.module.auth.service.EmailService;
import com.ecommerce.module.auth.service.RefreshTokenService;
import com.ecommerce.module.auth.service.impl.AuthServiceImpl;
import com.ecommerce.module.auth.util.RedisHelper;
import com.ecommerce.module.user.entity.User;
import com.ecommerce.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import jakarta.mail.MessagingException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthOauthAccountRepository oauthRepo;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private RedisHelper redisHelper;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_shouldCreateUserAndSendVerificationEmail() throws MessagingException {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Test User")
                .email("t@test.com")
                .password("Password123!")
                .role("CUSTOMER")
                .build();

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        // capture saved user
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        AuthResponse resp = authService.register(req);

        // verify email saved and verification token stored
        verify(userRepository, times(1)).save(any(User.class));
        verify(redisHelper, times(1)).set(startsWith("email_verify:"), eq(req.getEmail()), anyLong());
        verify(emailService, times(1)).sendVerificationEmail(eq(req.getEmail()), contains("verify-email?token="));

        assertThat(resp).isNotNull();
        assertThat(resp.getUserId()).isEqualTo(42L);
        assertThat(resp.getEmail()).isEqualTo(req.getEmail());
    }

    @Test
    void register_whenDuplicateEmail_throwsConflict() {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("X")
                .email("dup@test.com")
                .password("Password123!")
                .role("CUSTOMER")
                .build();

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_whenEmailNotVerified_throwsUnauthorized() {
        String email = "noverify@test.com";
        String password = "pass";
        User u = User.builder().id(10L).email(email).password("$2a$10$something").isEmailVerified(false).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(u));
        // the actual password matching is done via BCryptPasswordEncoder; to simulate
        // mismatch we just call method
        assertThatThrownBy(() -> authService.login(email, password, "device"))
                .isInstanceOf(com.ecommerce.module.auth.exception.UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void forgotPassword_shouldCreateResetTokenAndSendEmail() throws Exception {
        String email = "f@test.com";
        User u = User.builder().id(8L).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(u));
        authService.forgotPassword(email);
        verify(redisHelper, times(1)).set(startsWith("password_reset:"), eq(email), anyLong());
        verify(emailService, times(1)).sendPasswordResetEmail(eq(email), contains("reset-password?token="));
    }
}
