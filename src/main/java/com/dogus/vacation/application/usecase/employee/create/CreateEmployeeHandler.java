package com.dogus.vacation.application.usecase.employee.create;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.common.mapper.BalanceMapper;
import com.dogus.vacation.application.common.mapper.EmployeeMapper;
import com.dogus.vacation.application.common.view.BalanceView;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.service.LeaveBalanceService;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yeni çalışanı ve bu yılın ilk bakiyelerini tek transaction'da kaydeder (FR-06, T4).
 *
 * <p>Adımlar: e-posta tekil mi (409), departman ve yönetici var mı (404), çalışanı kaydet,
 * sonra {@link LeaveBalanceService#openYearBalances} ile bakiyeleri aç (yıllık izin kıdem ve
 * yaşa göre, mazeret 5 gün; hastalık ve ücretsiz izne bakiye açılmaz). Bakiye adımı hata
 * verirse çalışan da oluşmaz: ikisi aynı {@code @Transactional} metottadır.
 */
@Component
public class CreateEmployeeHandler implements RequestHandler<CreateEmployeeRequest, CreateEmployeeResponse> {

    private static final Logger log = LoggerFactory.getLogger(CreateEmployeeHandler.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveBalanceService balanceService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public CreateEmployeeHandler(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
                                 LeaveBalanceService balanceService, PasswordEncoder passwordEncoder,
                                 CurrentUserProvider currentUser, Clock clock) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.balanceService = balanceService;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreateEmployeeResponse handle(CreateEmployeeRequest request) {
        String email = request.email().strip();
        if (employeeRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE,
                    "'" + email + "' e-postası başka bir çalışanda kayıtlı.");
        }
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.DEPARTMENT_NOT_FOUND));
        Employee manager = request.managerId() == null ? null
                : employeeRepository.findById(request.managerId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Employee employee = new Employee(request.firstName().strip(), request.lastName().strip(), email,
                request.hireDate(), request.birthDate(), request.role(), department);
        employee.setManager(manager);
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        Employee saved = employeeRepository.save(employee);

        Employee hr = currentUser.currentEmployee();
        int year = LocalDate.now(clock).getYear();
        List<BalanceView> balances = balanceService.openYearBalances(saved, year, hr).stream()
                .map(BalanceMapper::toView)
                .toList();

        log.info("Çalışan eklendi: id={}, rol={}, departman={}, {} bakiye açıldı, ekleyen={}",
                saved.getId(), saved.getRole(), department.getId(), balances.size(), hr.getId());
        return new CreateEmployeeResponse(EmployeeMapper.toView(saved), balances);
    }
}
