package com.dogus.vacation.application.usecase.balance.employee;

import java.time.Clock;
import java.time.LocalDate;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.common.mapper.BalanceMapper;
import com.dogus.vacation.application.common.view.EmployeeRef;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.ForbiddenActionException;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bir çalışanın istenen yıldaki bakiyelerini döner (FR-32).
 *
 * <p><b>Yetki iki katmanlıdır.</b> Controller'daki {@code @PreAuthorize} yalnızca yönetici ve
 * İK rolünü içeri alır (normal çalışan kendi bakiyesini {@code /api/leave-balances/mine}'dan
 * görür). Hangi çalışanı görebileceği ise kayda bağlı bir kuraldır ve burada denetlenir:
 * İK herkesi, yönetici yalnızca doğrudan bağlı çalışanlarını görür; aksi halde
 * {@link ErrorCode#ACTION_NOT_ALLOWED} (403).
 */
@Component
public class GetEmployeeBalancesHandler
        implements RequestHandler<GetEmployeeBalancesRequest, GetEmployeeBalancesResponse> {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public GetEmployeeBalancesHandler(EmployeeRepository employeeRepository, LeaveBalanceRepository balanceRepository,
                                      CurrentUserProvider currentUser, Clock clock) {
        this.employeeRepository = employeeRepository;
        this.balanceRepository = balanceRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public GetEmployeeBalancesResponse handle(GetEmployeeBalancesRequest request) {
        Employee target = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND));
        Employee actor = currentUser.currentEmployee();
        if (!canSee(actor, target)) {
            throw new ForbiddenActionException(ErrorCode.ACTION_NOT_ALLOWED);
        }

        int year = request.year() != null ? request.year() : LocalDate.now(clock).getYear();
        var balances = balanceRepository.findWithLeaveTypeByEmployeeIdAndYear(target.getId(), year)
                .stream()
                .map(BalanceMapper::toView)
                .toList();
        return new GetEmployeeBalancesResponse(EmployeeRef.of(target), year, balances);
    }

    /** İK herkesi, yönetici yalnızca kendisine doğrudan bağlı çalışanları görür. */
    private static boolean canSee(Employee actor, Employee target) {
        if (actor.getRole() == Role.HR) {
            return true;
        }
        Employee manager = target.getManager();
        return actor.getRole() == Role.MANAGER && manager != null && manager.getId().equals(actor.getId());
    }
}
