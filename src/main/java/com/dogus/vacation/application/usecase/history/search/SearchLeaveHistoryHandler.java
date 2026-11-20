package com.dogus.vacation.application.usecase.history.search;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.mapper.HistoryEntryMapper;
import com.dogus.vacation.application.common.view.HistoryEntryView;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestHistoryRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestHistorySpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Şirket izin geçmişini filtreleyip sayfalı döner (FR-26); en yeni olay önce.
 *
 * <p>Tarih filtresi gün olarak gelir; şirketin saat dilimindeki (Clock bean'i,
 * Europe/Istanbul) gün başı ve ertesi günün başı olarak aralığa çevrilir. Böylece
 * {@code to} günü 23:59'daki olay da dahil olur.
 */
@Component
public class SearchLeaveHistoryHandler
        implements RequestHandler<SearchLeaveHistoryRequest, PageResponse<HistoryEntryView>> {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));

    private final LeaveRequestHistoryRepository historyRepository;
    private final Clock clock;

    public SearchLeaveHistoryHandler(LeaveRequestHistoryRepository historyRepository, Clock clock) {
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HistoryEntryView> handle(SearchLeaveHistoryRequest request) {
        var spec = LeaveRequestHistorySpecifications.matching(
                startOf(request.from()),
                startOf(request.to() == null ? null : request.to().plusDays(1)),
                request.departmentId(),
                request.status(),
                request.employeeId());
        var pageable = PageRequest.of(request.page(), request.size(), NEWEST_FIRST);
        return PageResponse.of(historyRepository.findAll(spec, pageable), HistoryEntryMapper::toView);
    }

    private OffsetDateTime startOf(LocalDate day) {
        return day == null ? null : day.atStartOfDay(clock.getZone()).toOffsetDateTime();
    }
}
