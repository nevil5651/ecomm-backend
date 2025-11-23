package com.ecommerce.module.auth.unit;

import com.ecommerce.module.auth.entity.AuthOauthAccount;
import com.ecommerce.module.auth.repository.AuthOauthAccountRepository;
import com.ecommerce.module.auth.service.impl.AuthServiceImpl;
import com.ecommerce.module.user.entity.User;
import com.ecommerce.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthServiceTest {

    @Mock
    private AuthOauthAccountRepository oauthRepo;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.ecommerce.module.auth.service.RefreshTokenService refreshService;
    @Mock
    private com.ecommerce.module.auth.service.EmailService emailService;
    @Mock
    private com.ecommerce.module.auth.util.RedisHelper redisHelper;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void oauthLogin_createsAndLinks_whenNoUserExists() {
        String provider = "google", providerId = "g-123", email = "new@e.com";
        when(oauthRepo.findByProviderAndProviderUserId(provider, providerId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(55L);
            return u;
        });

        var resp = authService.oauthLogin(provider, providerId, email, "CUSTOMER", "dev");
        assertThat(resp).isNotNull();
        assertThat(resp.getUserId()).isEqualTo(55L);
        verify(oauthRepo, times(1)).save(any(AuthOauthAccount.class));
    }
}
