package com.dogus.vacation.application.usecase.leavetype.create;

import com.dogus.vacation.application.common.mapper.LeaveTypeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yeni izin türünü aktif olarak kaydeder (FR-12).
 *
 * <p>Kod ve ad tekil olmalıdır; çakışma veritabanının tekil kısıtına takılıp 500 olarak
 * dönmesin diye önceden kontrol edilir ve {@link ErrorCode#DUPLICATE_VALUE} (409) verilir.
 */
@Component
public class CreateLeaveTypeHandler implements RequestHandler<CreateLeaveTypeRequest, CreateLeaveTypeResponse> {

    private final LeaveTypeRepository leaveTypeRepository;

    public CreateLeaveTypeHandler(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Override
    @Transactional
    public CreateLeaveTypeResponse handle(CreateLeaveTypeRequest request) {
        String name = request.name().strip();
        if (leaveTypeRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE,
                    "'" + request.code() + "' kodlu bir izin türü zaten var.");
        }
        if (leaveTypeRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE,
                    "'" + name + "' adlı bir izin türü zaten var.");
        }
        LeaveType saved = leaveTypeRepository.save(new LeaveType(
                request.code(), name, request.deductsFromBalance(), request.annualLimitDays()));
        return new CreateLeaveTypeResponse(LeaveTypeMapper.toView(saved));
    }
}
