package com.dogus.vacation.application.usecase.employee.update;

import com.dogus.vacation.application.common.mapper.EmployeeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.application.usecase.employee.HrMinimumRule;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışanın bilgilerini günceller (FR-07).
 *
 * <p>Sıra: çalışan var mı (404); e-posta başka çalışanda mı (409); departman ve yönetici var mı
 * (404); yeni yönetici çalışanın kendi ekibinden mi (döngü, 422 {@code MANAGER_CYCLE}); son iki
 * aktif İK'dan birinin rolü İK dışına mı çıkıyor (BR-13, 422). Hepsi geçerse alanlar yazılır.
 *
 * <p>İşe giriş veya doğum tarihi değişse bile açılmış bakiyeler değiştirilmez; yeni hak bir
 * sonraki yıllık hak edişte (T3) hesaplanır.
 */
@Component
public class UpdateEmployeeHandler implements RequestHandler<UpdateEmployeeRequest, UpdateEmployeeResponse> {

    private static final Logger log = LoggerFactory.getLogger(UpdateEmployeeHandler.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public UpdateEmployeeHandler(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional
    public UpdateEmployeeResponse handle(UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(request.id())
                .orElseThrow(() -> new NotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND));
        String email = request.email().strip();
        employeeRepository.findByEmailIgnoreCase(email)
                .filter(other -> !other.getId().equals(employee.getId()))
                .ifPresent(other -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_VALUE,
                            "'" + email + "' e-postası başka bir çalışanda kayıtlı.");
                });
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.DEPARTMENT_NOT_FOUND));
        Employee manager = request.managerId() == null ? null
                : employeeRepository.findById(request.managerId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND));
        ensureNoManagerCycle(employee, manager);
        if (request.role() != Role.HR) {
            HrMinimumRule.ensureAnotherHrRemains(employee, employeeRepository);
        }

        employee.setFirstName(request.firstName().strip());
        employee.setLastName(request.lastName().strip());
        employee.setEmail(email);
        employee.setHireDate(request.hireDate());
        employee.setBirthDate(request.birthDate());
        employee.setRole(request.role());
        employee.setDepartment(department);
        employee.setManager(manager);
        log.info("Çalışan güncellendi: id={}, rol={}, departman={}", employee.getId(), request.role(),
                department.getId());
        return new UpdateEmployeeResponse(EmployeeMapper.toView(employee));
    }

    /**
     * Yeni yöneticiden yukarı doğru yönetici zinciri izlenir; çalışanın kendisine ulaşılırsa
     * çalışan kendi ekibindeki birine bağlanıyor demektir (A, B'nin yöneticisiyken B'yi A'nın
     * yöneticisi yapmak). Zincirdeki kayıtlar zaten döngüsüz olduğu için izleme sonlanır.
     */
    private static void ensureNoManagerCycle(Employee employee, Employee newManager) {
        for (Employee current = newManager; current != null; current = current.getManager()) {
            if (current.getId().equals(employee.getId())) {
                throw new BusinessException(ErrorCode.MANAGER_CYCLE);
            }
        }
    }
}
