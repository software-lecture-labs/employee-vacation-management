package com.dogus.vacation.application.usecase.leavetype.update;

import com.dogus.vacation.application.common.view.LeaveTypeView;

/**
 * @param leaveType güncellenmiş izin türü
 */
public record UpdateLeaveTypeResponse(LeaveTypeView leaveType) {
}
