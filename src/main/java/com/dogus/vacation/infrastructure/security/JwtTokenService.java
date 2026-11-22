package com.dogus.vacation.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;

import com.dogus.vacation.application.common.auth.AccessTokenIssuer;
import com.dogus.vacation.application.common.auth.IssuedToken;
import com.dogus.vacation.domain.model.Employee;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * HS256 ile imzalı JWT üretir (FR-04).
 *
 * <p>Claim'ler: {@code sub} = çalışan id'si, {@code role} = rol, {@code email},
 * {@code iat} ve {@code exp}. Token'da şifre veya kişisel ayrıntı bulunmaz; ayrıntılar
 * gerektiğinde {@code CurrentUserProvider} ile veritabanından okunur.
 */
@Component
public class JwtTokenService implements AccessTokenIssuer {

    static final String ISSUER = "employee-vacation-management";
    static final String EMAIL_CLAIM = "email";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(Employee employee) {
        Objects.requireNonNull(employee.getId(), "Kaydedilmemiş çalışana token verilemez");
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(Duration.ofMinutes(properties.expirationMinutes()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(employee.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(EMAIL_CLAIM, employee.getEmail())
                .claim(SecurityConfig.ROLE_CLAIM, employee.getRole().name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedToken(token, OffsetDateTime.ofInstant(expiresAt, clock.getZone()));
    }
}
