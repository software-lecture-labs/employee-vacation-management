package com.dogus.vacation.api;

import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.cancellation.approve.ApproveLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.approve.ApproveLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.cancel.CancelLeaveRequest;
import com.dogus.vacation.application.usecase.cancellation.cancel.CancelLeaveResponse;
import com.dogus.vacation.application.usecase.cancellation.reject.RejectLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.reject.RejectLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.request.RequestLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.request.RequestLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.withdraw.WithdrawLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.withdraw.WithdrawLeaveCancellationResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Onaylı iznin iptal endpoint'leri (FR-23, FR-25, FR-28, FR-29).
 *
 * <p>İki yol vardır: yönetici veya İK izni şirket adına doğrudan iptal eder; ya da talep sahibi
 * iptal talebi açar, onaylayıcı kabul veya reddeder, talep sahibi vazgeçebilir. Günler yalnızca
 * iptal gerçekleşince (doğrudan iptal ya da kabul) iade edilir (BR-17, T2).
 *
 * <p><b>Yetki:</b> rol kontrolü burada ({@code @PreAuthorize}); "bu talebe bu kişi mi karar verir"
 * ve "talep bu kişinin mi" kontrolleri Handler'dadır. Controller iş kuralı içermez.
 */
@RestController
@RequestMapping("/api/leave-requests/{id}")
public class LeaveCancellationController {

    private final Mediator mediator;

    public LeaveCancellationController(Mediator mediator) {
        this.mediator = mediator;
    }

    /** Onaylı izni şirket adına iptal eder; gerekçe zorunlu. Günler aynı transaction'da iade edilir. */
    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public CancelLeaveResponse cancel(@PathVariable Long id, @RequestBody CancelLeaveRequest request) {
        return mediator.send(request.withId(id));
    }

    /** Talep sahibi onaylı izni için iptal talebi açar; gerekçe zorunlu. */
    @PostMapping("/cancel-request")
    public RequestLeaveCancellationResponse requestCancellation(
            @PathVariable Long id, @RequestBody RequestLeaveCancellationRequest request) {
        return mediator.send(request.withId(id));
    }

    /** Onaylayıcı iptal talebini kabul eder; izin iptal olur, günler iade edilir. */
    @PostMapping("/cancel-request/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ApproveLeaveCancellationResponse approveCancellation(@PathVariable Long id) {
        return mediator.send(new ApproveLeaveCancellationRequest(id));
    }

    /** Onaylayıcı iptal talebini reddeder; izin onaylı kalır. Açıklama isteğe bağlı. */
    @PostMapping("/cancel-request/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public RejectLeaveCancellationResponse rejectCancellation(
            @PathVariable Long id, @RequestBody(required = false) RejectLeaveCancellationRequest request) {
        RejectLeaveCancellationRequest body = request == null ? new RejectLeaveCancellationRequest(id, null) : request;
        return mediator.send(body.withId(id));
    }

    /** Talep sahibi iptal talebinden vazgeçer; izin onaylı kalır. */
    @PostMapping("/cancel-request/withdraw")
    public WithdrawLeaveCancellationResponse withdrawCancellation(@PathVariable Long id) {
        return mediator.send(new WithdrawLeaveCancellationRequest(id));
    }
}
