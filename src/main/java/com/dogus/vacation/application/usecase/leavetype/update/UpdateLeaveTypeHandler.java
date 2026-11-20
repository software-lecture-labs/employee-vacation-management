package com.dogus.vacation.application.usecase.leavetype.update;

import java.util.List;

import com.dogus.vacation.application.common.mapper.LeaveTypeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * İzin türünün adını, yıllık sınırını ve aktifliğini günceller (FR-12).
 *
 * <p>"Bakiyeden düşmeyen türde sınır boş olmalı" kuralı türün kayıtlı halini bilmeyi
 * gerektirdiği için Validator'da değil burada kontrol edilir; ekleme ile aynı cevabı
 * (400, alan hatası) vermek için {@link ValidationException} fırlatılır.
 */
@Component
public class UpdateLeaveTypeHandler implements RequestHandler<UpdateLeaveTypeRequest, UpdateLeaveTypeResponse> {

    static final String LIMIT_WITHOUT_BALANCE = "Bakiyeden düşmeyen türde yıllık sınır boş olmalı.";

    private final LeaveTypeRepository leaveTypeRepository;

    public UpdateLeaveTypeHandler(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Override
    @Transactional
    public UpdateLeaveTypeResponse handle(UpdateLeaveTypeRequest request) {
        LeaveType leaveType = leaveTypeRepository.findById(request.id())
                .orElseThrow(() -> new NotFoundException(ErrorCode.LEAVE_TYPE_NOT_FOUND));

        if (!leaveType.isDeductsFromBalance() && request.annualLimitDays() != null) {
            throw new ValidationException(List.of(new ValidationError("annualLimitDays", LIMIT_WITHOUT_BALANCE)));
        }
        String name = request.name().strip();
        if (leaveTypeRepository.existsByNameAndIdNot(name, leaveType.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE,
                    "'" + name + "' adlı bir izin türü zaten var.");
        }

        leaveType.setName(name);
        leaveType.setAnnualLimitDays(request.annualLimitDays());
        leaveType.setActive(request.active());
        return new UpdateLeaveTypeResponse(LeaveTypeMapper.toView(leaveType));
    }
}
