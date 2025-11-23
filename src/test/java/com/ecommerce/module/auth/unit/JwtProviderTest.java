package com.ecommerce.module.auth.unit;

import com.ecommerce.module.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    @Test
    void createAndParseToken() {
        JwtTokenProvider provider = new JwtTokenProvider();
        // set secret and expiry using reflection (since provider reads @Value in real
        // app)
        ReflectionTestUtils.setField(provider, "accessSecret",
                "a_very_long_secret_for_testing_purpose_at_least_256_bits_a".repeat(2));
        ReflectionTestUtils.setField(provider, "accessExpirySeconds", 3600L);

        String token = provider.createAccessToken(123L, "x@e.com", List.of("CUSTOMER"));
        assertThat(token).isNotBlank();

        Jws<Claims> parsed = provider.parse(token);
        Claims claims = parsed.getBody();
        assertThat(claims.getSubject()).isEqualTo("123");
        assertThat(claims.get("email")).isEqualTo("x@e.com");
    }
}
