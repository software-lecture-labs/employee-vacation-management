package com.dogus.vacation.application.usecase.cancellation;

import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestRepository;
import org.springframework.stereotype.Component;

/**
 * İptal akışındaki beş Handler'ın ortak ilk adımı: talebi id'siyle bulur, yoksa 404 verir.
 */
@Component
public class LeaveRequestFinder {

    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequestFinder(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * @throws NotFoundException {@link ErrorCode#LEAVE_REQUEST_NOT_FOUND}
     */
    public LeaveRequest find(Long leaveRequestId) {
        return leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));
    }
}
