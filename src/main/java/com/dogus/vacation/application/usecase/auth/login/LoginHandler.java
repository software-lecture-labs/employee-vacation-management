package com.dogus.vacation.application.usecase.auth.login;

import java.util.Optional;

import com.dogus.vacation.application.common.auth.AccessTokenIssuer;
import com.dogus.vacation.application.common.auth.IssuedToken;
import com.dogus.vacation.application.common.mapper.EmployeeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * E-posta ve şifreyi doğrular, aktif çalışana token verir (FR-01, FR-02, BR-10).
 *
 * <p>Kayıtlı olmayan e-posta, yanlış şifre ve pasif çalışan aynı cevabı alır
 * (401, {@link ErrorCode#INVALID_CREDENTIALS}); böylece dışarıdan hangi e-postanın
 * kayıtlı olduğu anlaşılamaz. Aynı nedenle e-posta kayıtlı değilken de bir BCrypt
 * karşılaştırması yapılır ve cevap süresi benzer kalır.
 */
@Component
public class LoginHandler implements RequestHandler<LoginRequest, LoginResponse> {

    static final String TOKEN_TYPE = "Bearer";

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer tokenIssuer;

    /** Kayıtlı olmayan e-postada karşılaştırılan, hiçbir şifreye ait olmayan hash. */
    private final String unknownUserHash;

    public LoginHandler(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
                        AccessTokenIssuer tokenIssuer) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.unknownUserHash = passwordEncoder.encode("unknown-user-" + System.nanoTime());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse handle(LoginRequest request) {
        Optional<Employee> found = employeeRepository.findByEmailIgnoreCase(request.email().strip());
        String hash = found.map(Employee::getPasswordHash).orElse(unknownUserHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !passwordMatches || !found.get().isActive()) {
            log.info("Başarısız giriş denemesi");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Employee employee = found.get();
        IssuedToken token = tokenIssuer.issue(employee);
        log.info("Giriş yapıldı: çalışan {}", employee.getId());
        return new LoginResponse(token.value(), TOKEN_TYPE, token.expiresAt(), EmployeeMapper.toView(employee));
    }
}
