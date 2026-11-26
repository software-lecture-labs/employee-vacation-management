package com.dogus.vacation.application.usecase.department.list;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Departmanları ada göre sıralı ve sayfalı listeleme isteği (FR-10; giriş yapmış herkes).
 *
 * <p>Sayfalama sınırları alan anotasyonlarıyla denetlenir; alanlar arası kural
 * {@link ListDepartmentsValidator}'dadır. Cevap {@link ListDepartmentsResponse}, ortak
 * sayfa yapısını ({@code PageResponse}) sarar.
 *
 * @param page sayfa, 0'dan başlar; boşsa 0
 * @param size sayfa büyüklüğü, 1–100; boşsa 20 (NFR-07)
 */
public record ListDepartmentsRequest(
        @Min(value = 0, message = "Sayfa numarası negatif olamaz.")
        Integer page,

        @Min(value = 1, message = "Sayfa büyüklüğü en az 1 olmalı.")
        @Max(value = 100, message = "Sayfa büyüklüğü en fazla 100 olabilir.")
        Integer size)
        implements Request<ListDepartmentsResponse> {

    /** Varsayılan sayfa büyüklüğü (NFR-07). */
    public static final int DEFAULT_SIZE = 20;

    public ListDepartmentsRequest {
        page = (page == null) ? 0 : page;
        size = (size == null) ? DEFAULT_SIZE : size;
    }
}
