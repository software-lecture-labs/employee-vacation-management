package com.dogus.vacation.application.usecase.cancellation.reject;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Onaylayıcının iptal talebini reddetmesi (FR-28). İzin onaylı kalır, günler iade edilmez
 * (BR-17). Açıklama isteğe bağlıdır; verilirse geçmişe yazılır.
 *
 * <p>{@code leaveRequestId} yoldan gelir; Controller gövdeyi {@link #withId(Long)} ile tamamlar.
 *
 * @param leaveRequestId izin talebi
 * @param reason         isteğe bağlı açıklama; en fazla 500 karakter
 */
public record RejectLeaveCancellationRequest(
        @NotNull(message = "İzin talebi belirtilmeli.")
        @Positive(message = "Geçerli bir izin talebi seçilmeli.")
        Long leaveRequestId,

        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.")
        String reason)
        implements Request<RejectLeaveCancellationResponse> {

    /** Gövdeden gelen isteğe yoldaki id'yi ekler; gövdede id olsa bile yoldaki geçerlidir. */
    public RejectLeaveCancellationRequest withId(Long pathId) {
        return new RejectLeaveCancellationRequest(pathId, reason);
    }
}
