package com.dogus.vacation.application.usecase.department.update;

import com.dogus.vacation.application.common.mapper.DepartmentMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Departmanın adını günceller (FR-10). Departman yoksa 404, ad başka bir departmanda
 * kullanılıyorsa 409 döner. Departman silinmez, bu yüzden silme use case'i yoktur.
 */
@Component
public class UpdateDepartmentHandler implements RequestHandler<UpdateDepartmentRequest, UpdateDepartmentResponse> {

    private final DepartmentRepository departmentRepository;

    public UpdateDepartmentHandler(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional
    public UpdateDepartmentResponse handle(UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(request.id())
                .orElseThrow(() -> new NotFoundException(ErrorCode.DEPARTMENT_NOT_FOUND));
        String name = request.name().strip();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, department.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "'" + name + "' adlı bir departman zaten var.");
        }
        department.setName(name);
        return new UpdateDepartmentResponse(DepartmentMapper.toView(department));
    }
}
