package com.dogus.vacation.application.usecase.cancellation.reject;

import com.dogus.vacation.application.common.view.LeaveRequestDetailView;

/**
 * @param leaveRequest talebin güncel hali (durum, iptal bilgileri)
 */
public record RejectLeaveCancellationResponse(LeaveRequestDetailView leaveRequest) {
}
