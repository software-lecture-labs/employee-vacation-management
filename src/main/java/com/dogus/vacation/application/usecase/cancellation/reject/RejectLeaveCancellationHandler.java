package com.dogus.vacation.application.usecase.cancellation.reject;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.common.mapper.LeaveRequestMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.application.usecase.cancellation.LeaveRequestFinder;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.domain.policy.ApproverResolver;
import com.dogus.vacation.domain.service.LeaveHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Onaylayıcı iptal talebini reddeder (FR-28, BR-17). İzin APPROVED'a döner; günler zaten
 * düşülmüş olduğu için bakiyede değişiklik olmaz. Geçmişe aynı transaction'da yazılır.
 * Karar yetkisi onaydakiyle aynıdır ({@link ApproverResolver}).
 */
@Component
public class RejectLeaveCancellationHandler
        implements RequestHandler<RejectLeaveCancellationRequest, RejectLeaveCancellationResponse> {

    private static final Logger log = LoggerFactory.getLogger(RejectLeaveCancellationHandler.class);

    private final LeaveRequestFinder finder;
    private final ApproverResolver approverResolver;
    private final LeaveHistoryService historyService;
    private final CurrentUserProvider currentUser;

    public RejectLeaveCancellationHandler(LeaveRequestFinder finder, ApproverResolver approverResolver,
                                          LeaveHistoryService historyService, CurrentUserProvider currentUser) {
        this.finder = finder;
        this.approverResolver = approverResolver;
        this.historyService = historyService;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public RejectLeaveCancellationResponse handle(RejectLeaveCancellationRequest request) {
        Employee actor = currentUser.currentEmployee();
        LeaveRequest leave = finder.find(request.leaveRequestId());
        approverResolver.checkCanDecide(actor, leave.getEmployee());

        LeaveRequestStatus from = leave.getStatus();
        leave.rejectCancellationRequest();
        historyService.record(leave, from, actor, request.reason());

        log.info("İptal talebi reddedildi: talep={}, reddeden={}", leave.getId(), actor.getId());
        return new RejectLeaveCancellationResponse(LeaveRequestMapper.toDetailView(leave));
    }
}
