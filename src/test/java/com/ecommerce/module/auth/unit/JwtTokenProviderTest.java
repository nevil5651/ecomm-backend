package com.ecommerce.module.auth.unit;

import com.ecommerce.module.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
    
public class JwtTokenProviderTest {

    @Test
    void createAndParseToken() {
        JwtTokenProvider provider = new JwtTokenProvider();
        // set secret via reflection for test (must be 256+ bits in bytes)
        ReflectionTestUtils.setField(provider, "accessSecret",
                "replace_with_256_bit_minimum_string_for_tests_only________________________");
        ReflectionTestUtils.setField(provider, "accessExpirySeconds", 3600L);

        String token = provider.createAccessToken(123L, "a@b.com", List.of("CUSTOMER"));
        assertThat(token).isNotBlank();

        Jws<Claims> parsed = provider.parse(token);
        assertThat(parsed.getBody().getSubject()).isEqualTo("123");
        assertThat(parsed.getBody().get("email")).isEqualTo("a@b.com");
    }
}
