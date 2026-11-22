package com.dogus.vacation.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private static final String SECRET_32_BYTES = "0123456789abcdef0123456789abcdef";

    @Test
    void acceptsSecretOf32Bytes() {
        JwtProperties properties = new JwtProperties(SECRET_32_BYTES, 60);

        assertThat(properties.secretKey().getEncoded()).hasSize(32);
        assertThat(properties.secretKey().getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void rejectsMissingOrShortSecretSoTheAppDoesNotStart() {
        assertThatThrownBy(() -> new JwtProperties(null, 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtProperties("${JWT_SECRET}", 60))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES.substring(1), 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveExpiration() {
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_EXPIRATION_MINUTES");
    }

    @Test
    void toStringHidesTheSecret() {
        assertThat(new JwtProperties(SECRET_32_BYTES, 60).toString())
                .doesNotContain(SECRET_32_BYTES)
                .contains("***");
    }
}
