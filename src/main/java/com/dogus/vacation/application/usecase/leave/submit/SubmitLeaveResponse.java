package com.dogus.vacation.application.usecase.leave.submit;

import com.dogus.vacation.application.common.view.LeaveRequestDetailView;

/**
 * Açılan talebin ayrıntıları; durum {@code PENDING}.
 *
 * @param leave açılan talep
 */
public record SubmitLeaveResponse(LeaveRequestDetailView leave) {
}
