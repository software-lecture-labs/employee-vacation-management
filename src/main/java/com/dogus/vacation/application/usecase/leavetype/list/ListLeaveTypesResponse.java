package com.dogus.vacation.application.usecase.leavetype.list;

import java.util.List;

import com.dogus.vacation.application.common.view.LeaveTypeView;

/**
 * İzin türleri, id sırasıyla.
 *
 * @param leaveTypes aktif ve pasif tüm türler
 */
public record ListLeaveTypesResponse(List<LeaveTypeView> leaveTypes) {
}
