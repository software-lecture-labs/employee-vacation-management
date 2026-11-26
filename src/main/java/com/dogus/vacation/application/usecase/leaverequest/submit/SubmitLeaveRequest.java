package com.dogus.vacation.application.usecase.leaverequest.submit;

import java.time.LocalDate;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Giriş yapan çalışanın yeni izin talebi (FR-14). Talep sahibi istekte yoktur;
 * Handler onu {@code CurrentUserProvider}'dan alır.
 *
 * <pre>{@code
 * { "leaveTypeId": 1, "startDate": "2027-02-05", "endDate": "2027-02-09", "reason": "Aile ziyareti" }
 * }</pre>
 *
 * @param leaveTypeId izin türü
 * @param startDate   başlangıç (dahil)
 * @param endDate     bitiş (dahil)
 * @param reason      açıklama; isteğe bağlı
 */
public record SubmitLeaveRequest(
        @NotNull(message = "İzin türü seçilmeli.")
        @Positive(message = "Geçerli bir izin türü seçilmeli.")
        Long leaveTypeId,

        @NotNull(message = "Başlangıç tarihi zorunlu.")
        LocalDate startDate,

        @NotNull(message = "Bitiş tarihi zorunlu.")
        LocalDate endDate,

        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.")
        String reason)
        implements Request<SubmitLeaveResponse> {
}
