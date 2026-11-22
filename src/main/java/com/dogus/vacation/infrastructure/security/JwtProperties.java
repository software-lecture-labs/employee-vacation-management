package com.dogus.vacation.infrastructure.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT ayarları ({@code app.security.jwt.*}). Değerler ortam değişkeninden gelir
 * ({@code JWT_SECRET}, {@code JWT_EXPIRATION_MINUTES}); koda yazılmaz (NFR-04).
 *
 * <p>Anahtar eksik ya da kısaysa uygulama açılmaz: HS256 en az 32 baytlık anahtar ister
 * ve kısa anahtarla imzalanmış token güvenli değildir.
 *
 * @param secret            HS256 imzalama anahtarı, en az 32 bayt
 * @param expirationMinutes token süresi (dakika), pozitif
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, int expirationMinutes) {

    static final int MIN_SECRET_BYTES = 32;
    private static final String HMAC_SHA_256 = "HmacSHA256";

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "JWT_SECRET en az " + MIN_SECRET_BYTES + " bayt olmalı; .env dosyasını kontrol edin.");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_MINUTES pozitif bir sayı olmalı.");
        }
    }

    /** @return token imzalama ve doğrulamada kullanılan anahtar */
    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
    }

    /** Anahtar loglara veya hata mesajlarına düşmesin. */
    @Override
    public String toString() {
        return "JwtProperties[secret=***, expirationMinutes=" + expirationMinutes + "]";
    }
}
