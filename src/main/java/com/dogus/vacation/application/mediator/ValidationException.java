package com.dogus.vacation.application.mediator;

import java.util.List;

/**
 * İstek doğrulamadan geçemediğinde fırlatılır.
 *
 * <p>Mediator bu hatayı Handler'ı çağırmadan önce atar; bu yüzden hiçbir veri
 * yazılmaz. Global hata yönetimi bunu 400 ve {@code fieldErrors} listesine çevirir.
 */
public class ValidationException extends RuntimeException {

    private final transient List<ValidationError> errors;

    /**
     * @param errors bulunan hatalar; boş olamaz
     */
    public ValidationException(List<ValidationError> errors) {
        super("İstek geçersiz.");
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("En az bir doğrulama hatası olmalı");
        }
        this.errors = List.copyOf(errors);
    }

    /** @return değiştirilemez hata listesi */
    public List<ValidationError> getErrors() {
        return errors;
    }
}
