package com.dogus.vacation.application.usecase.cancellation.cancel;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Onaylı izni şirket adına iptal etme isteği (FR-23; izni onaylayabilecek yönetici veya İK).
 * İzin başlamadan önce yapılabilir (BR-08); gerekçe zorunludur (BR-09).
 *
 * <p>{@code leaveRequestId} yoldan gelir; Controller gövdeyi {@link #withId(Long)} ile tamamlar.
 *
 * @param leaveRequestId izin talebi
 * @param reason         iptal gerekçesi; talep sahibine ve geçmişe yazılır; en fazla 500 karakter
 */
public record CancelLeaveRequest(
        @NotNull(message = "İzin talebi belirtilmeli.")
        @Positive(message = "Geçerli bir izin talebi seçilmeli.")
        Long leaveRequestId,

        @NotBlank(message = "İptal gerekçesi zorunlu.")
        @Size(max = 500, message = "İptal gerekçesi en fazla 500 karakter olabilir.")
        String reason)
        implements Request<CancelLeaveResponse> {

    /** Gövdeden gelen isteğe yoldaki id'yi ekler; gövdede id olsa bile yoldaki geçerlidir. */
    public CancelLeaveRequest withId(Long pathId) {
        return new CancelLeaveRequest(pathId, reason);
    }
}
