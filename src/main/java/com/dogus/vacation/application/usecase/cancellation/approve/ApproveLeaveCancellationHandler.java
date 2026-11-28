package com.dogus.vacation.application.usecase.cancellation.approve;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.common.mapper.LeaveRequestMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.application.usecase.cancellation.LeaveRequestFinder;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.domain.policy.ApproverResolver;
import com.dogus.vacation.domain.service.LeaveBalanceService;
import com.dogus.vacation.domain.service.LeaveHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Onaylayıcı iptal talebini kabul eder (FR-28, BR-17, T2).
 *
 * <p>Karar yetkisi onaydakiyle aynıdır ({@link ApproverResolver}); talep sahibi kendi iptal
 * talebini kabul edemez (403). İzin başladıysa kabul edilemez (409). Durum CANCELLED olur;
 * geçmiş satırı ve gün iadesi aynı transaction'dadır: bir adım hata verirse talep
 * CANCEL_REQUESTED kalır ve gün iade edilmez.
 */
@Component
public class ApproveLeaveCancellationHandler
        implements RequestHandler<ApproveLeaveCancellationRequest, ApproveLeaveCancellationResponse> {

    private static final Logger log = LoggerFactory.getLogger(ApproveLeaveCancellationHandler.class);

    private final LeaveRequestFinder finder;
    private final ApproverResolver approverResolver;
    private final LeaveHistoryService historyService;
    private final LeaveBalanceService balanceService;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public ApproveLeaveCancellationHandler(LeaveRequestFinder finder, ApproverResolver approverResolver,
                                           LeaveHistoryService historyService, LeaveBalanceService balanceService,
                                           CurrentUserProvider currentUser, Clock clock) {
        this.finder = finder;
        this.approverResolver = approverResolver;
        this.historyService = historyService;
        this.balanceService = balanceService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ApproveLeaveCancellationResponse handle(ApproveLeaveCancellationRequest request) {
        Employee actor = currentUser.currentEmployee();
        LeaveRequest leave = finder.find(request.leaveRequestId());
        approverResolver.checkCanDecide(actor, leave.getEmployee());

        LeaveRequestStatus from = leave.getStatus();
        leave.approveCancellationRequest(actor, LocalDate.now(clock), OffsetDateTime.now(clock));
        historyService.record(leave, from, actor, leave.getCancelReason());
        balanceService.refund(leave, actor);

        log.info("İptal talebi kabul edildi: talep={}, çalışan={}, gün={}, kabul eden={}",
                leave.getId(), leave.getEmployee().getId(), leave.getWorkingDays(), actor.getId());
        return new ApproveLeaveCancellationResponse(LeaveRequestMapper.toDetailView(leave));
    }
}
