package com.dogus.vacation.application.usecase.leavetype.create;

import com.dogus.vacation.application.common.view.LeaveTypeView;

/**
 * @param leaveType eklenen izin türü
 */
public record CreateLeaveTypeResponse(LeaveTypeView leaveType) {
}
