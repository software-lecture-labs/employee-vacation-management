package com.dogus.vacation.application.usecase.cancellation.request;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Talep sahibinin onaylı izni için iptal talebi açması (FR-25). İzin başlamadan önce
 * açılabilir (BR-08); gerekçe zorunludur (BR-09). Kararı onaylayıcı verir (FR-28).
 *
 * <p>{@code leaveRequestId} yoldan gelir; Controller gövdeyi {@link #withId(Long)} ile tamamlar.
 *
 * @param leaveRequestId izin talebi
 * @param reason         neden iptal istendiği; en fazla 500 karakter
 */
public record RequestLeaveCancellationRequest(
        @NotNull(message = "İzin talebi belirtilmeli.")
        @Positive(message = "Geçerli bir izin talebi seçilmeli.")
        Long leaveRequestId,

        @NotBlank(message = "İptal gerekçesi zorunlu.")
        @Size(max = 500, message = "İptal gerekçesi en fazla 500 karakter olabilir.")
        String reason)
        implements Request<RequestLeaveCancellationResponse> {

    /** Gövdeden gelen isteğe yoldaki id'yi ekler; gövdede id olsa bile yoldaki geçerlidir. */
    public RequestLeaveCancellationRequest withId(Long pathId) {
        return new RequestLeaveCancellationRequest(pathId, reason);
    }
}
