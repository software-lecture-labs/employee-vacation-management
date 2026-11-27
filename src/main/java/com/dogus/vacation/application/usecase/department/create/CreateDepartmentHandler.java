package com.dogus.vacation.application.usecase.department.create;

import com.dogus.vacation.application.common.mapper.DepartmentMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yeni departmanı kaydeder (FR-10).
 *
 * <p>Ad tekil olmalıdır; çakışma veritabanının tekil kısıtına takılıp 500 olarak dönmesin
 * diye önceden, büyük/küçük harfe bakmadan kontrol edilir ve
 * {@link ErrorCode#DUPLICATE_VALUE} (409) verilir.
 */
@Component
public class CreateDepartmentHandler implements RequestHandler<CreateDepartmentRequest, CreateDepartmentResponse> {

    private final DepartmentRepository departmentRepository;

    public CreateDepartmentHandler(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional
    public CreateDepartmentResponse handle(CreateDepartmentRequest request) {
        String name = request.name().strip();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "'" + name + "' adlı bir departman zaten var.");
        }
        Department saved = departmentRepository.save(new Department(name));
        return new CreateDepartmentResponse(DepartmentMapper.toView(saved));
    }
}
