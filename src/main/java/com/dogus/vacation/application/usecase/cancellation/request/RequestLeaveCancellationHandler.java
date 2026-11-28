package com.dogus.vacation.application.usecase.cancellation.request;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.common.mapper.LeaveRequestMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.application.usecase.cancellation.LeaveRequestFinder;
import com.dogus.vacation.domain.exception.ForbiddenActionException;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.domain.service.LeaveHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Talep sahibi onaylı izni için iptal talebi açar (FR-25). Yalnızca talep sahibi açabilir
 * (başkası 403); izin APPROVED olmalı ve başlamamış olmalı (BR-08, aksi halde 409).
 * Durum CANCEL_REQUESTED olur ve geçmişe aynı transaction'da yazılır; günler kabulde iade edilir.
 */
@Component
public class RequestLeaveCancellationHandler
        implements RequestHandler<RequestLeaveCancellationRequest, RequestLeaveCancellationResponse> {

    private static final Logger log = LoggerFactory.getLogger(RequestLeaveCancellationHandler.class);

    private final LeaveRequestFinder finder;
    private final LeaveHistoryService historyService;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public RequestLeaveCancellationHandler(LeaveRequestFinder finder, LeaveHistoryService historyService,
                                           CurrentUserProvider currentUser, Clock clock) {
        this.finder = finder;
        this.historyService = historyService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RequestLeaveCancellationResponse handle(RequestLeaveCancellationRequest request) {
        Employee actor = currentUser.currentEmployee();
        LeaveRequest leave = finder.find(request.leaveRequestId());
        if (!leave.isOwnedBy(actor)) {
            throw new ForbiddenActionException();
        }

        LeaveRequestStatus from = leave.getStatus();
        leave.requestCancellation(request.reason(), LocalDate.now(clock), OffsetDateTime.now(clock));
        historyService.record(leave, from, actor, leave.getCancelRequestReason());

        log.info("İptal talebi açıldı: talep={}, çalışan={}", leave.getId(), actor.getId());
        return new RequestLeaveCancellationResponse(LeaveRequestMapper.toDetailView(leave));
    }
}
