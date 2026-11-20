package com.dogus.vacation.application.usecase.leavetype.list;

import com.dogus.vacation.application.common.mapper.LeaveTypeMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tüm izin türlerini id sırasıyla döner. Tür sayısı az olduğu için sayfalama yoktur.
 * Doğrulanacak alan olmadığından Validator'ı yoktur.
 */
@Component
public class ListLeaveTypesHandler implements RequestHandler<ListLeaveTypesRequest, ListLeaveTypesResponse> {

    private final LeaveTypeRepository leaveTypeRepository;

    public ListLeaveTypesHandler(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ListLeaveTypesResponse handle(ListLeaveTypesRequest request) {
        return new ListLeaveTypesResponse(leaveTypeRepository.findAll(Sort.by("id")).stream()
                .map(LeaveTypeMapper::toView)
                .toList());
    }
}
