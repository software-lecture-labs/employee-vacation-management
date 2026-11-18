package com.dogus.vacation.application.mediator;

import java.util.List;

/**
 * Bir isteği veritabanına bakmadan, sadece kendi alanlarıyla doğrular.
 *
 * <p>Tek alanlık kurallar (zorunlu, uzunluk) istek record'unda Jakarta
 * anotasyonlarıyla yazılır; alanlar arası kurallar (bitiş ≥ başlangıç gibi)
 * {@code XxxValidator} sınıfında yazılır. Veritabanı gerektiren kurallar buraya
 * konmaz, Handler'da transaction içinde kontrol edilir.
 *
 * @param <Q> doğrulanan istek tipi
 */
public interface RequestValidator<Q extends Request<?>> {

    /**
     * @param request doğrulanacak istek
     * @return bulunan hatalar; istek geçerliyse boş liste
     */
    List<ValidationError> validate(Q request);
}
