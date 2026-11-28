package com.dogus.vacation.application.usecase.cancellation.withdraw;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Talep sahibinin iptal talebinden vazgeçmesi (FR-29). İzin onaylı kalır.
 *
 * @param leaveRequestId izin talebi
 */
public record WithdrawLeaveCancellationRequest(
        @NotNull(message = "İzin talebi belirtilmeli.")
        @Positive(message = "Geçerli bir izin talebi seçilmeli.")
        Long leaveRequestId)
        implements Request<WithdrawLeaveCancellationResponse> {
}
