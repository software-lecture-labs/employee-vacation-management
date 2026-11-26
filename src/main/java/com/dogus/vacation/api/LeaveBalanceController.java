package com.dogus.vacation.api;

import com.dogus.vacation.api.docs.LeaveBalanceApi;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.balance.accrual.RunAnnualAccrualRequest;
import com.dogus.vacation.application.usecase.balance.accrual.RunAnnualAccrualResponse;
import com.dogus.vacation.application.usecase.balance.employee.GetEmployeeBalancesRequest;
import com.dogus.vacation.application.usecase.balance.employee.GetEmployeeBalancesResponse;
import com.dogus.vacation.application.usecase.balance.mine.GetMyBalancesRequest;
import com.dogus.vacation.application.usecase.balance.mine.GetMyBalancesResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * İzin bakiyesi uç noktaları (FR-32, FR-34).
 *
 * <p>Controller iş kuralı içermez: isteği Request record'una çevirir ve mediator'a verir.
 * Rol kontrolü burada, kayda bağlı yetki (yönetici yalnızca kendi ekibi) Handler'dadır.
 *
 * <p>Swagger açıklamaları ve örnekleri {@link LeaveBalanceApi}'dedir.
 */
@RestController
public class LeaveBalanceController implements LeaveBalanceApi {

    private final Mediator mediator;

    public LeaveBalanceController(Mediator mediator) {
        this.mediator = mediator;
    }

    /** Giriş yapan çalışanın bakiyeleri (giriş yapmış herkes). {@code year} boşsa bu yıl. */
    @Override
    @GetMapping("/api/leave-balances/mine")
    public GetMyBalancesResponse mine(@RequestParam(required = false) Integer year) {
        return mediator.send(new GetMyBalancesRequest(year));
    }

    /** Bir çalışanın bakiyeleri: yönetici yalnızca kendi ekibini, İK herkesi görür. */
    @Override
    @GetMapping("/api/employees/{id}/leave-balances")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public GetEmployeeBalancesResponse ofEmployee(@PathVariable Long id,
                                                  @RequestParam(required = false) Integer year) {
        return mediator.send(new GetEmployeeBalancesRequest(id, year));
    }

    /**
     * Yıllık hak edişi çalıştırır (İK, T3). Bütün aktif çalışanlara o yılın bakiyeleri açılır;
     * aynı yıl ikinci kez çalıştırılırsa 409. {@code year} burada zorunlu yazılmaz, boş gelirse
     * istekteki {@code @NotNull} diğer doğrulama hatalarıyla aynı biçimde 400 döner.
     */
    @Override
    @PostMapping("/api/leave-balances/accruals")
    @PreAuthorize("hasRole('HR')")
    @ResponseStatus(HttpStatus.CREATED)
    public RunAnnualAccrualResponse runAccrual(@RequestParam(required = false) Integer year) {
        return mediator.send(new RunAnnualAccrualRequest(year));
    }
}
