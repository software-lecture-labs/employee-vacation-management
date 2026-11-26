package com.dogus.vacation.application.usecase.department.list;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.mapper.DepartmentMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Departmanları ada göre sıralı döner (FR-10). Aynı sayfa iki istekte aynı sırada
 * gelsin diye ikinci sıralama ölçütü id'dir; adlar tekil olduğu için pratikte
 * yalnızca ada göre sıralanır.
 */
@Component
public class ListDepartmentsHandler
        implements RequestHandler<ListDepartmentsRequest, ListDepartmentsResponse> {

    static final Sort ORDER = Sort.by("name", "id");

    private final DepartmentRepository departmentRepository;

    public ListDepartmentsHandler(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ListDepartmentsResponse handle(ListDepartmentsRequest request) {
        PageRequest pageRequest = PageRequest.of(request.page(), request.size(), ORDER);
        return new ListDepartmentsResponse(
                PageResponse.of(departmentRepository.findAll(pageRequest), DepartmentMapper::toView));
    }
}
