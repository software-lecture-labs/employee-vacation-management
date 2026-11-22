package com.dogus.vacation.infrastructure.security;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * {@link CurrentUserProvider}'ın REST uygulaması: kimliği isteğin JWT'sinden okur
 * ({@code sub} = çalışan id'si).
 *
 * <p>{@code SecurityContextHolder} yalnızca bu sınıfta okunur; Handler'lar Spring
 * Security'yi bilmez (plan 9.2).
 */
@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

    private final EmployeeRepository employeeRepository;

    public JwtCurrentUserProvider(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Long currentEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Long.valueOf(jwtAuthentication.getToken().getSubject());
        }
        throw new IllegalStateException("Giriş yapmış kullanıcı yok; bu işlem token'lı bir istekte çağrılmalı.");
    }

    @Override
    public Employee currentEmployee() {
        Long id = currentEmployeeId();
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }
}
