package com.dogus.vacation.application.usecase.balance.carryover;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.service.LeaveBalanceService;
import com.dogus.vacation.infrastructure.persistence.AnnualAccrualRunRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kullanılmayan yıllık izni biten yıldan bu yıla devreder (BR-12, bonus).
 *
 * <p>Akış:
 * <ol>
 *   <li>Bu yılın hak edişi (T3) çalıştırılmamışsa {@link ErrorCode#ACCRUAL_NOT_RUN} (409):
 *       günlerin ekleneceği yeni yıl bakiyeleri henüz yoktur.</li>
 *   <li>İşlemi yapan İK pasifse {@link ErrorCode#INACTIVE_EMPLOYEE} (BR-10).</li>
 *   <li>Aktif çalışanların iki yıldaki yıllık izin bakiyeleri birer sorguyla okunur.</li>
 *   <li>Kalanı olan her bakiye {@link LeaveBalanceService#carryOver} ile yeni yıla devredilir.
 *       Yeni yıl bakiyesi olmayan çalışan atlanır ve sayısı cevapta döner.</li>
 * </ol>
 *
 * <p>Yalnızca yıllık izin devreder; mazeret izni "yılda en fazla 5 gün" kuralı (BR-04) yüzünden
 * devretmez. Üst sınır yoktur: kullanılmayan yıllık izin yanmaz.
 *
 * <p><b>Transaction sınırı bu metottur;</b> bir çalışanda hata olursa hiçbir devir kalmaz.
 * Devir ikinci kez çalıştırılırsa eski yılın kalanları sıfır olduğu için hiçbir şey değişmez.
 * Aynı anda iki çalıştırmada bakiyelerin iyimser kilidi ({@code @Version}) ikincisini durdurur.
 */
@Component
public class RunCarryOverHandler implements RequestHandler<RunCarryOverRequest, RunCarryOverResponse> {

    private final AnnualAccrualRunRepository runRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final LeaveBalanceService balanceService;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public RunCarryOverHandler(AnnualAccrualRunRepository runRepository, LeaveTypeRepository leaveTypeRepository,
                               LeaveBalanceRepository balanceRepository, LeaveBalanceService balanceService,
                               CurrentUserProvider currentUser, Clock clock) {
        this.runRepository = runRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.balanceRepository = balanceRepository;
        this.balanceService = balanceService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RunCarryOverResponse handle(RunCarryOverRequest request) {
        int fromYear = request.fromYear();
        int toYear = fromYear + 1;
        if (!runRepository.existsByYear(toYear)) {
            throw new BusinessException(ErrorCode.ACCRUAL_NOT_RUN,
                    toYear + " yılının hak edişi çalıştırılmadan devir yapılamaz.");
        }
        Employee hr = currentUser.currentEmployee();
        if (!hr.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_EMPLOYEE);
        }
        LeaveType annual = leaveTypeRepository.findByCode(LeaveType.ANNUAL_CODE)
                .orElseThrow(() -> new NotFoundException(ErrorCode.LEAVE_TYPE_NOT_FOUND));

        Map<Long, LeaveBalance> newYearBalances = balanceRepository.findForActiveEmployees(annual, toYear).stream()
                .collect(Collectors.toMap(balance -> balance.getEmployee().getId(), Function.identity()));
        int carriedEmployees = 0;
        int skippedEmployees = 0;
        BigDecimal carriedDays = BigDecimal.ZERO;
        List<LeaveBalance> withRemainingDays = balanceRepository.findForActiveEmployees(annual, fromYear).stream()
                .filter(balance -> balance.remainingDays().signum() > 0)
                .toList();
        for (LeaveBalance oldYear : withRemainingDays) {
            LeaveBalance newYear = newYearBalances.get(oldYear.getEmployee().getId());
            if (newYear == null) {
                skippedEmployees++;
            } else {
                carriedDays = carriedDays.add(balanceService.carryOver(oldYear, newYear, hr));
                carriedEmployees++;
            }
        }
        return new RunCarryOverResponse(fromYear, toYear, carriedEmployees, carriedDays, skippedEmployees,
                OffsetDateTime.now(clock));
    }
}
