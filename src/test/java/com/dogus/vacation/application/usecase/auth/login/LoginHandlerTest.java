package com.dogus.vacation.application.usecase.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.dogus.vacation.application.common.auth.AccessTokenIssuer;
import com.dogus.vacation.application.common.auth.IssuedToken;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Giriş kuralları: doğru bilgiyle token; kayıtsız e-posta, yanlış şifre ve pasif çalışan
 * aynı 401 kodunu alır (FR-01, BR-10). Veritabanı ve gerçek BCrypt yok (Mockito).
 */
class LoginHandlerTest {

    private static final String PASSWORD = "Sifre123!";
    private static final String HASH = "$2a$10$hash-of-Sifre123";
    private static final IssuedToken TOKEN =
            new IssuedToken("header.payload.signature", OffsetDateTime.parse("2026-11-22T12:00:00+03:00"));

    private EmployeeRepository employeeRepository;
    private PasswordEncoder passwordEncoder;
    private AccessTokenIssuer tokenIssuer;
    private LoginHandler handler;
    private Employee ayse;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenIssuer = mock(AccessTokenIssuer.class);
        handler = new LoginHandler(employeeRepository, passwordEncoder, tokenIssuer);

        ayse = TestData.employee(TestData.department(), Role.HR);
        ayse.setPasswordHash(HASH);
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(tokenIssuer.issue(ayse)).thenReturn(TOKEN);
    }

    @Test
    void correctCredentialsReturnBearerTokenAndProfile() {
        when(employeeRepository.findByEmailIgnoreCase(ayse.getEmail())).thenReturn(Optional.of(ayse));

        LoginResponse response = handler.handle(new LoginRequest(ayse.getEmail(), PASSWORD));

        assertThat(response.accessToken()).isEqualTo(TOKEN.value());
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isEqualTo(TOKEN.expiresAt());
        assertThat(response.employee().email()).isEqualTo(ayse.getEmail());
        assertThat(response.employee().role()).isEqualTo(Role.HR);
    }

    @Test
    void emailIsTrimmedBeforeLookup() {
        when(employeeRepository.findByEmailIgnoreCase(ayse.getEmail())).thenReturn(Optional.of(ayse));

        LoginResponse response = handler.handle(new LoginRequest("  " + ayse.getEmail() + " ", PASSWORD));

        assertThat(response.accessToken()).isEqualTo(TOKEN.value());
    }

    @Test
    void wrongPasswordIs401() {
        when(employeeRepository.findByEmailIgnoreCase(ayse.getEmail())).thenReturn(Optional.of(ayse));

        assertInvalidCredentials(new LoginRequest(ayse.getEmail(), "YanlisSifre"));
    }

    @Test
    void unknownEmailIs401AndStillComparesAPassword() {
        when(employeeRepository.findByEmailIgnoreCase("yok@test.local")).thenReturn(Optional.empty());

        assertInvalidCredentials(new LoginRequest("yok@test.local", PASSWORD));
        verify(passwordEncoder).matches(eq(PASSWORD), ArgumentMatchers.<String>any());
    }

    @Test
    void inactiveEmployeeCannotSignInEvenWithCorrectPassword() {
        ayse.setActive(false);
        when(employeeRepository.findByEmailIgnoreCase(ayse.getEmail())).thenReturn(Optional.of(ayse));

        assertInvalidCredentials(new LoginRequest(ayse.getEmail(), PASSWORD));
    }

    @Test
    void passwordIsNotPrintedInLogs() {
        assertThat(new LoginRequest(ayse.getEmail(), PASSWORD).toString()).doesNotContain(PASSWORD);
    }

    private void assertInvalidCredentials(LoginRequest request) {
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(tokenIssuer, never()).issue(ArgumentMatchers.any());
    }
}
