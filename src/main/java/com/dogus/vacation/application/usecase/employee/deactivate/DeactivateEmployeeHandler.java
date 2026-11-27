package com.dogus.vacation.application.usecase.employee.deactivate;

import com.dogus.vacation.application.common.mapper.EmployeeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.application.usecase.employee.HrMinimumRule;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışanı pasife alır (FR-08). Kayıt ve geçmişi silinmez.
 *
 * <p>Son iki aktif İK'dan biri pasife alınamaz (BR-13, 422). Zaten pasif olan çalışan için
 * işlem bir şey değiştirmeden aynı cevabı döner; aynı istek iki kez gönderilirse hata çıkmaz.
 */
@Component
public class DeactivateEmployeeHandler
        implements RequestHandler<DeactivateEmployeeRequest, DeactivateEmployeeResponse> {

    private static final Logger log = LoggerFactory.getLogger(DeactivateEmployeeHandler.class);

    private final EmployeeRepository employeeRepository;

    public DeactivateEmployeeHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public DeactivateEmployeeResponse handle(DeactivateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(request.id())
                .orElseThrow(() -> new NotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND));
        if (employee.isActive()) {
            HrMinimumRule.ensureAnotherHrRemains(employee, employeeRepository);
            employee.setActive(false);
            log.info("Çalışan pasife alındı: id={}, rol={}", employee.getId(), employee.getRole());
        }
        return new DeactivateEmployeeResponse(EmployeeMapper.toView(employee));
    }
}
