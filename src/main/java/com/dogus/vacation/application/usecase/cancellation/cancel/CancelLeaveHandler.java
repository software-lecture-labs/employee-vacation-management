package com.dogus.vacation.application.usecase.cancellation.cancel;

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
 * Onaylı izni şirket adına iptal eder (FR-23, T2).
 *
 * <p>Yetki (BR-15): izni onaylayabilecek kişi iptal edebilir; çalışanın yöneticisi, yöneticisi
 * yoksa bir İK, İK'nın iznini başka bir İK ({@link ApproverResolver}, BR-07 ile aynı kural).
 * Kendi iznini şirket adına iptal edemez; bunun için iptal talebi açar (FR-25).
 *
 * <p><b>T2:</b> durum değişikliği, geçmiş satırı ve gün iadesi aynı transaction'dadır. Herhangi
 * bir adım hata verirse izin APPROVED kalır ve gün iade edilmez. Bekleyen bir iptal talebi
 * varsa o da bu işlemle kapanır.
 */
@Component
public class CancelLeaveHandler implements RequestHandler<CancelLeaveRequest, CancelLeaveResponse> {

    private static final Logger log = LoggerFactory.getLogger(CancelLeaveHandler.class);

    private final LeaveRequestFinder finder;
    private final ApproverResolver approverResolver;
    private final LeaveHistoryService historyService;
    private final LeaveBalanceService balanceService;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public CancelLeaveHandler(LeaveRequestFinder finder, ApproverResolver approverResolver,
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
    public CancelLeaveResponse handle(CancelLeaveRequest request) {
        Employee actor = currentUser.currentEmployee();
        LeaveRequest leave = finder.find(request.leaveRequestId());
        approverResolver.checkCanDecide(actor, leave.getEmployee());

        LeaveRequestStatus from = leave.getStatus();
        leave.cancel(actor, request.reason(), LocalDate.now(clock), OffsetDateTime.now(clock));
        historyService.record(leave, from, actor, leave.getCancelReason());
        balanceService.refund(leave, actor);

        log.info("İzin şirket adına iptal edildi: talep={}, çalışan={}, gün={}, iptal eden={}",
                leave.getId(), leave.getEmployee().getId(), leave.getWorkingDays(), actor.getId());
        return new CancelLeaveResponse(LeaveRequestMapper.toDetailView(leave));
    }
}
