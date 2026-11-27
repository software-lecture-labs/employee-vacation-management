package com.dogus.vacation.application.usecase.employee.get;

import com.dogus.vacation.application.common.mapper.EmployeeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bir çalışanın detayını döner (FR-09). Pasif çalışan da görüntülenebilir; yoksa 404.
 */
@Component
public class GetEmployeeHandler implements RequestHandler<GetEmployeeRequest, GetEmployeeResponse> {

    private final EmployeeRepository employeeRepository;

    public GetEmployeeHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GetEmployeeResponse handle(GetEmployeeRequest request) {
        return employeeRepository.findById(request.id())
                .map(EmployeeMapper::toView)
                .map(GetEmployeeResponse::new)
                .orElseThrow(() -> new NotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }
}
