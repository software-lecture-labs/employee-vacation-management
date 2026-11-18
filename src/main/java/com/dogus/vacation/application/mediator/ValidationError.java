package com.dogus.vacation.application.mediator;

import java.util.Objects;

/**
 * Doğrulamada bulunan tek bir hata.
 *
 * <p>Hata cevabındaki {@code fieldErrors} listesinin bir elemanına karşılık gelir.
 *
 * @param field   hatalı alanın adı (istek record'undaki adıyla, örn. {@code endDate})
 * @param message kullanıcıya gösterilecek Türkçe mesaj
 */
public record ValidationError(String field, String message) {

    public ValidationError {
        Objects.requireNonNull(field, "field boş olamaz");
        Objects.requireNonNull(message, "message boş olamaz");
    }
}
