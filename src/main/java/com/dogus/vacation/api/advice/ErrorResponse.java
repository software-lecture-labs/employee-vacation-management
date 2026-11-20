package com.dogus.vacation.api.advice;

import java.time.OffsetDateTime;
import java.util.List;

import com.dogus.vacation.application.mediator.ValidationError;

/**
 * Bütün hataların tek JSON biçimi (NFR-05). Stack trace veya sınıf adı taşımaz.
 *
 * <pre>{@code
 * { "timestamp": "2027-01-20T10:05:31+03:00", "status": 422, "code": "INSUFFICIENT_BALANCE",
 *   "message": "Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün.",
 *   "path": "/api/leave-requests/43/approve", "fieldErrors": [] }
 * }</pre>
 *
 * @param timestamp   hatanın oluştuğu an (Europe/Istanbul)
 * @param status      HTTP durum kodu
 * @param code        istemcinin karar vereceği sabit kod ({@code VALIDATION_FAILED},
 *                    {@code ErrorCode} adları veya HTTP durumunun adı)
 * @param message     kullanıcıya gösterilecek Türkçe mesaj
 * @param path        isteğin adresi
 * @param fieldErrors doğrulama hatalarında alan bazında hatalar; diğerlerinde boş liste
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<ValidationError> fieldErrors) {

    public ErrorResponse {
        fieldErrors = (fieldErrors == null) ? List.of() : List.copyOf(fieldErrors);
    }
}
