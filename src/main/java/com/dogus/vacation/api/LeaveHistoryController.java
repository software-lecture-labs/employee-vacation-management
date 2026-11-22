package com.dogus.vacation.api;

import java.time.LocalDate;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.HistoryEntryView;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.history.search.SearchLeaveHistoryRequest;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Şirket izin geçmişi (FR-26).
 *
 * <p><b>Yetki:</b> Yalnızca İK ({@code @PreAuthorize("hasRole('HR')")}); başka rol 403 alır.
 */
@RestController
public class LeaveHistoryController {

    private final Mediator mediator;

    public LeaveHistoryController(Mediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Filtreli ve sayfalı geçmiş; en yeni olay önce. Tarihler {@code yyyy-MM-dd} biçiminde.
     * Örnek: {@code /api/leave-requests/history?from=2026-11-01&to=2026-11-30&status=APPROVED}
     */
    @GetMapping("/api/leave-requests/history")
    @PreAuthorize("hasRole('HR')")
    public PageResponse<HistoryEntryView> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return mediator.send(new SearchLeaveHistoryRequest(from, to, departmentId, status, employeeId, page, size));
    }
}
