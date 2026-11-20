package com.dogus.vacation.application.usecase.history.search;

import java.time.LocalDate;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.HistoryEntryView;
import com.dogus.vacation.application.mediator.Request;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * İK'nın şirket izin geçmişi araması (FR-26). Filtrelerin hepsi isteğe bağlıdır.
 *
 * @param from         olay tarihi bu günden itibaren (dahil)
 * @param to           olay tarihi bu güne kadar (dahil)
 * @param departmentId talep sahibinin departmanı
 * @param status       geçişin yeni durumu (APPROVED, REJECTED ...)
 * @param employeeId   talep sahibi
 * @param page         sayfa, 0'dan başlar; boşsa 0
 * @param size         sayfa büyüklüğü, 1–100; boşsa 20 (NFR-07)
 */
public record SearchLeaveHistoryRequest(
        LocalDate from,
        LocalDate to,

        @Positive(message = "Geçerli bir departman seçilmeli.")
        Long departmentId,

        LeaveRequestStatus status,

        @Positive(message = "Geçerli bir çalışan seçilmeli.")
        Long employeeId,

        @Min(value = 0, message = "Sayfa numarası negatif olamaz.")
        Integer page,

        @Min(value = 1, message = "Sayfa büyüklüğü en az 1 olmalı.")
        @Max(value = 100, message = "Sayfa büyüklüğü en fazla 100 olabilir.")
        Integer size)
        implements Request<PageResponse<HistoryEntryView>> {

    /** Varsayılan sayfa büyüklüğü (NFR-07). */
    public static final int DEFAULT_SIZE = 20;

    public SearchLeaveHistoryRequest {
        page = (page == null) ? 0 : page;
        size = (size == null) ? DEFAULT_SIZE : size;
    }
}
