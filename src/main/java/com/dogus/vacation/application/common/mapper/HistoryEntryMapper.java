package com.dogus.vacation.application.common.mapper;

import com.dogus.vacation.application.common.view.EmployeeRef;
import com.dogus.vacation.application.common.view.HistoryEntryView;
import com.dogus.vacation.application.common.view.LeaveTypeRef;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveRequestHistory;

/** {@link LeaveRequestHistory} satırını {@link HistoryEntryView}'a çevirir. */
public final class HistoryEntryMapper {

    private HistoryEntryMapper() {
    }

    public static HistoryEntryView toView(LeaveRequestHistory entry) {
        LeaveRequest request = entry.getLeaveRequest();
        return new HistoryEntryView(
                entry.getId(),
                entry.getOccurredAt(),
                request.getId(),
                EmployeeRef.of(request.getEmployee()),
                request.getEmployee().getDepartment().getName(),
                LeaveTypeRef.of(request.getLeaveType()),
                request.getStartDate(),
                request.getEndDate(),
                entry.getFromStatus(),
                entry.getToStatus(),
                EmployeeRef.of(entry.getActor()),
                entry.getNote());
    }
}
