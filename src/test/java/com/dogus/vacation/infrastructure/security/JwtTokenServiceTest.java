package com.dogus.vacation.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.dogus.vacation.application.common.auth.IssuedToken;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Üretilen token'ın aynı anahtarla doğrulandığını, claim'lerini ve süresini kontrol eder.
 * Spring context açılmaz; encoder ve decoder {@link SecurityConfig}'teki bean metotlarıyla kurulur.
 */
class JwtTokenServiceTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final JwtProperties PROPERTIES =
            new JwtProperties("test-only-jwt-signing-key-not-used-in-any-environment", 60);

    /** Decoder süreyi gerçek saate göre denetlediği için "şimdi"ye sabitlenir. */
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private JwtDecoder decoder;
    private JwtTokenService tokenService;
    private Employee hr;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        decoder = config.jwtDecoder(PROPERTIES);
        tokenService = new JwtTokenService(config.jwtEncoder(PROPERTIES), PROPERTIES, Clock.fixed(now, ISTANBUL));
        hr = TestData.employee(TestData.department(), Role.HR);
        ReflectionTestUtils.setField(hr, "id", 7L);
    }

    @Test
    void tokenCarriesEmployeeIdRoleAndEmail() {
        IssuedToken issued = tokenService.issue(hr);

        Jwt jwt = decoder.decode(issued.value());
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("HR");
        assertThat(jwt.getClaimAsString("email")).isEqualTo(hr.getEmail());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtTokenService.ISSUER);
    }

    @Test
    void tokenExpiresAfterConfiguredMinutes() {
        IssuedToken issued = tokenService.issue(hr);

        Jwt jwt = decoder.decode(issued.value());
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(60)));
        assertThat(issued.expiresAt().toInstant()).isEqualTo(now.plus(Duration.ofMinutes(60)));
        assertThat(issued.expiresAt().getOffset()).isEqualTo(ISTANBUL.getRules().getOffset(now));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtProperties otherKey = new JwtProperties("another-test-only-key-that-is-also-long-enough", 60);
        JwtTokenService otherService = new JwtTokenService(
                new SecurityConfig().jwtEncoder(otherKey), otherKey, Clock.fixed(now, ISTANBUL));
        String foreignToken = otherService.issue(hr).value();

        assertThatThrownBy(() -> decoder.decode(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        Instant twoHoursAgo = now.minus(Duration.ofHours(2));
        JwtTokenService pastService = new JwtTokenService(
                new SecurityConfig().jwtEncoder(PROPERTIES), PROPERTIES, Clock.fixed(twoHoursAgo, ISTANBUL));
        String expiredToken = pastService.issue(hr).value();

        assertThatThrownBy(() -> decoder.decode(expiredToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void unsavedEmployeeCannotGetToken() {
        Employee unsaved = TestData.employee(TestData.department());

        assertThatThrownBy(() -> tokenService.issue(unsaved)).isInstanceOf(NullPointerException.class);
    }
}
