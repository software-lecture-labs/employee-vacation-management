package com.dogus.vacation.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtCurrentUserProviderTest {

    private EmployeeRepository employeeRepository;
    private JwtCurrentUserProvider provider;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        provider = new JwtCurrentUserProvider(employeeRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsEmployeeIdFromTokenSubject() {
        signInAs("42");

        assertThat(provider.currentEmployeeId()).isEqualTo(42L);
    }

    @Test
    void loadsTheSignedInEmployee() {
        Employee ayse = TestData.employee(TestData.department());
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(ayse));
        signInAs("42");

        assertThat(provider.currentEmployee()).isSameAs(ayse);
    }

    @Test
    void missingEmployeeRecordIs404() {
        when(employeeRepository.findById(42L)).thenReturn(Optional.empty());
        signInAs("42");

        assertThatThrownBy(() -> provider.currentEmployee())
                .isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @Test
    void failsWhenNobodyIsSignedIn() {
        assertThatThrownBy(() -> provider.currentEmployeeId()).isInstanceOf(IllegalStateException.class);
    }

    private static void signInAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(subject).claim("role", "EMPLOYEE")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
