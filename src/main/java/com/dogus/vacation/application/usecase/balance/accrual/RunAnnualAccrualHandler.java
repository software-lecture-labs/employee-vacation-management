package com.dogus.vacation.application.usecase.balance.accrual;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.mediator.RequestHandler;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.AnnualAccrualRun;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.service.LeaveBalanceService;
import com.dogus.vacation.infrastructure.persistence.AnnualAccrualRunRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yıllık hak edişi çalıştırır (T3; FR-33, FR-34, BR-11).
 *
 * <p>Akış:
 * <ol>
 *   <li>Bu yıl daha önce çalıştırıldıysa {@link ErrorCode#ACCRUAL_ALREADY_RUN} (409). Kontrol
 *       bakiyelere değil {@code annual_accrual_runs} tablosuna bakar: yıl içinde eklenip T4 ile
 *       bakiyesi açılan bir çalışan, o yılın hak edişini yanlışlıkla engellememelidir.</li>
 *   <li>İşlemi yapan İK pasifse {@link ErrorCode#INACTIVE_EMPLOYEE} (BR-10).</li>
 *   <li>Her aktif çalışana {@link LeaveBalanceService#openYearBalances} ile bakiyeleri açılır;
 *       hangi türlerin kaç gün açılacağı ve kimin atlanacağı o metottadır.</li>
 *   <li>En sonda çalıştırma kaydı yazılır.</li>
 * </ol>
 *
 * <p><b>Transaction sınırı bu metottur.</b> {@link LeaveBalanceService} kendi transaction'ını
 * açmaz ({@code MANDATORY}), buradakine katılır. Bu yüzden 200 çalışandan 150'ncisinde bir hata
 * çıkarsa önceki 149 çalışanın bakiyeleri, ACCRUAL hareketleri ve çalıştırma kaydı birlikte geri
 * alınır; yarım kalmış bir hak ediş veritabanında kalmaz.
 *
 * <p><b>Aynı anda iki çalıştırma:</b> İki İK aynı anda başlatırsa ikisi de ilk kontrolü geçebilir.
 * Veritabanındaki {@code uq_annual_accrual_runs_year} kısıtı ikinci kaydı reddeder; bu hata
 * {@code saveAndFlush} ile burada yakalanır, aynı 409'a çevrilir ve ikinci işlemin açtığı bütün
 * bakiyeler geri alınır.
 */
@Component
public class RunAnnualAccrualHandler implements RequestHandler<RunAnnualAccrualRequest, RunAnnualAccrualResponse> {

    private final AnnualAccrualRunRepository runRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceService balanceService;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public RunAnnualAccrualHandler(AnnualAccrualRunRepository runRepository, EmployeeRepository employeeRepository,
                                   LeaveTypeRepository leaveTypeRepository, LeaveBalanceService balanceService,
                                   CurrentUserProvider currentUser, Clock clock) {
        this.runRepository = runRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.balanceService = balanceService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RunAnnualAccrualResponse handle(RunAnnualAccrualRequest request) {
        int year = request.year();
        if (runRepository.existsByYear(year)) {
            throw alreadyRun(year);
        }
        Employee hr = currentUser.currentEmployee();
        if (!hr.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_EMPLOYEE);
        }

        // Türler bir kez okunur; çalışan başına tekrar sorgulanmaz.
        List<LeaveType> leaveTypes = leaveTypeRepository.findAllByActiveTrueAndDeductsFromBalanceTrueOrderByIdAsc();
        int accruedEmployees = 0;
        int skippedEmployees = 0;
        int openedBalances = 0;
        for (Employee employee : employeeRepository.findAllByActiveTrueOrderByIdAsc()) {
            List<LeaveBalance> opened = balanceService.openYearBalances(employee, year, leaveTypes, hr);
            if (opened.isEmpty()) {
                skippedEmployees++;
            } else {
                accruedEmployees++;
                openedBalances += opened.size();
            }
        }

        OffsetDateTime runAt = OffsetDateTime.now(clock);
        try {
            runRepository.saveAndFlush(new AnnualAccrualRun(year, runAt, hr, accruedEmployees));
        } catch (DataIntegrityViolationException concurrentRun) {
            throw alreadyRun(year);
        }
        return new RunAnnualAccrualResponse(year, accruedEmployees, skippedEmployees, openedBalances, runAt);
    }

    private static BusinessException alreadyRun(int year) {
        return new BusinessException(ErrorCode.ACCRUAL_ALREADY_RUN,
                year + " yılının hak edişi zaten çalıştırıldı.");
    }
}
