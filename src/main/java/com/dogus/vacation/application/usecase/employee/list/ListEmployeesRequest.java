package com.dogus.vacation.application.usecase.employee.list;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * Çalışanları sayfalı ve filtreli listeleme isteği (FR-09; yalnızca İK).
 *
 * @param departmentId yalnızca bu departmandakiler; boşsa hepsi
 * @param active       {@code true} aktifler, {@code false} pasifler; boşsa hepsi
 * @param page         sayfa, 0'dan başlar; boşsa 0
 * @param size         sayfa büyüklüğü, 1–100; boşsa 20 (NFR-07)
 */
public record ListEmployeesRequest(
        @Positive(message = "Geçerli bir departman seçilmeli.")
        Long departmentId,

        Boolean active,

        @Min(value = 0, message = "Sayfa numarası negatif olamaz.")
        Integer page,

        @Min(value = 1, message = "Sayfa büyüklüğü en az 1 olmalı.")
        @Max(value = 100, message = "Sayfa büyüklüğü en fazla 100 olabilir.")
        Integer size)
        implements Request<ListEmployeesResponse> {

    /** Varsayılan sayfa büyüklüğü (NFR-07). */
    public static final int DEFAULT_SIZE = 20;

    public ListEmployeesRequest {
        page = (page == null) ? 0 : page;
        size = (size == null) ? DEFAULT_SIZE : size;
    }
}
