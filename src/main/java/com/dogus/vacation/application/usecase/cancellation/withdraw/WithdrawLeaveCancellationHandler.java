package com.dogus.vacation.application.usecase.cancellation.withdraw;

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
 * Talep sahibi iptal talebinden vazgeçer (FR-29). Yalnızca talep sahibi yapabilir (başkası 403);
 * izin APPROVED'a döner. Geçmişe aynı transaction'da yazılır.
 */
@Component
public class WithdrawLeaveCancellationHandler
        implements RequestHandler<WithdrawLeaveCancellationRequest, WithdrawLeaveCancellationResponse> {

    private static final Logger log = LoggerFactory.getLogger(WithdrawLeaveCancellationHandler.class);

    private final LeaveRequestFinder finder;
    private final LeaveHistoryService historyService;
    private final CurrentUserProvider currentUser;

    public WithdrawLeaveCancellationHandler(LeaveRequestFinder finder, LeaveHistoryService historyService,
                                            CurrentUserProvider currentUser) {
        this.finder = finder;
        this.historyService = historyService;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public WithdrawLeaveCancellationResponse handle(WithdrawLeaveCancellationRequest request) {
        Employee actor = currentUser.currentEmployee();
        LeaveRequest leave = finder.find(request.leaveRequestId());
        if (!leave.isOwnedBy(actor)) {
            throw new ForbiddenActionException();
        }

        LeaveRequestStatus from = leave.getStatus();
        leave.withdrawCancellationRequest();
        historyService.record(leave, from, actor, null);

        log.info("İptal talebinden vazgeçildi: talep={}, çalışan={}", leave.getId(), actor.getId());
        return new WithdrawLeaveCancellationResponse(LeaveRequestMapper.toDetailView(leave));
    }
}
