package com.dogus.vacation.application.usecase.balance.mine;

import java.time.Clock;
import java.time.LocalDate;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.common.mapper.BalanceMapper;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giriş yapan çalışanın istenen yıldaki bakiyelerini döner (FR-32).
 *
 * <p>Yalnızca token'daki çalışan id'si kullanılır ({@link CurrentUserProvider#currentEmployeeId()});
 * çalışan kaydı ayrıca yüklenmez. Yıl verilmezse şirket saatine göre bu yıl alınır.
 */
@Component
public class GetMyBalancesHandler implements RequestHandler<GetMyBalancesRequest, GetMyBalancesResponse> {

    private final LeaveBalanceRepository balanceRepository;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public GetMyBalancesHandler(LeaveBalanceRepository balanceRepository, CurrentUserProvider currentUser,
                                Clock clock) {
        this.balanceRepository = balanceRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public GetMyBalancesResponse handle(GetMyBalancesRequest request) {
        int year = request.year() != null ? request.year() : LocalDate.now(clock).getYear();
        var balances = balanceRepository.findWithLeaveTypeByEmployeeIdAndYear(currentUser.currentEmployeeId(), year)
                .stream()
                .map(BalanceMapper::toView)
                .toList();
        return new GetMyBalancesResponse(year, balances);
    }
}
