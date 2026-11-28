package com.dogus.vacation.application.usecase.cancellation.approve;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Onaylayıcının iptal talebini kabul etmesi (FR-28). İzin iptal olur ve günler aynı
 * transaction'da bakiyeye iade edilir (BR-17, T2).
 *
 * @param leaveRequestId izin talebi
 */
public record ApproveLeaveCancellationRequest(
        @NotNull(message = "İzin talebi belirtilmeli.")
        @Positive(message = "Geçerli bir izin talebi seçilmeli.")
        Long leaveRequestId)
        implements Request<ApproveLeaveCancellationResponse> {
}
