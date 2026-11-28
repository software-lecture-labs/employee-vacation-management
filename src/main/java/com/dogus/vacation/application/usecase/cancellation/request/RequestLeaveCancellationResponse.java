package com.dogus.vacation.application.usecase.cancellation.request;

import com.dogus.vacation.application.common.view.LeaveRequestDetailView;

/**
 * @param leaveRequest talebin güncel hali (durum, iptal bilgileri)
 */
public record RequestLeaveCancellationResponse(LeaveRequestDetailView leaveRequest) {
}
