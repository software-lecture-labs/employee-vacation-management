package com.dogus.vacation.application.common.mapper;

import com.dogus.vacation.application.common.view.EmployeeRef;
import com.dogus.vacation.application.common.view.LeaveRequestDetailView;
import com.dogus.vacation.application.common.view.LeaveRequestView;
import com.dogus.vacation.application.common.view.LeaveTypeRef;
import com.dogus.vacation.domain.model.LeaveRequest;

/**
 * {@link LeaveRequest} entity'sini cevap view'larına çevirir.
 *
 * <p>Handler içinde, transaction açıkken çağrılır (open-in-view kapalı); entity hiçbir
 * zaman Controller'a çıkmaz.
 */
public final class LeaveRequestMapper {

    private LeaveRequestMapper() {
    }

    /** @return listeler için özet */
    public static LeaveRequestView toView(LeaveRequest leave) {
        return new LeaveRequestView(
                leave.getId(),
                EmployeeRef.of(leave.getEmployee()),
                LeaveTypeRef.of(leave.getLeaveType()),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getWorkingDays(),
                leave.getStatus());
    }

    /** @return bütün ayrıntılar */
    public static LeaveRequestDetailView toDetailView(LeaveRequest leave) {
        return new LeaveRequestDetailView(
                leave.getId(),
                EmployeeRef.of(leave.getEmployee()),
                LeaveTypeRef.of(leave.getLeaveType()),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getWorkingDays(),
                leave.getReason(),
                leave.getStatus(),
                EmployeeRef.of(leave.getApprover()),
                leave.getDecisionNote(),
                leave.getDecidedAt(),
                leave.getCancelRequestReason(),
                leave.getCancelRequestedAt(),
                EmployeeRef.of(leave.getCancelledBy()),
                leave.getCancelReason(),
                leave.getCancelledAt());
    }
}
