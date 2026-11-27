package com.dogus.vacation.application.usecase.employee.list;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.mapper.EmployeeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışanları departmana ve aktifliğe göre filtreleyip soyad, ad ve id sırasıyla döner (FR-09).
 * Departman ve yönetici aynı sorguda yüklenir ({@code EmployeeRepository#findAll}).
 */
@Component
public class ListEmployeesHandler implements RequestHandler<ListEmployeesRequest, ListEmployeesResponse> {

    static final Sort ORDER = Sort.by("lastName", "firstName", "id");

    private final EmployeeRepository employeeRepository;

    public ListEmployeesHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ListEmployeesResponse handle(ListEmployeesRequest request) {
        PageRequest pageRequest = PageRequest.of(request.page(), request.size(), ORDER);
        return new ListEmployeesResponse(PageResponse.of(
                employeeRepository.findAll(
                        EmployeeSpecifications.matching(request.departmentId(), request.active()), pageRequest),
                EmployeeMapper::toView));
    }
}
