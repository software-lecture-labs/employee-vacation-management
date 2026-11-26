package com.dogus.vacation.application.usecase.balance.carryover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.AnnualAccrualRun;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.AnnualAccrualRunRepository;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Kullanılmayan yıllık iznin devrini (BR-12) mediator üzerinden, gerçek PostgreSQL ile çalıştırır.
 *
 * <p>Devir geçen yıldan bu yıla yapılır (Validator'ın kabul ettiği tek yıl). İşlemi yapan İK
 * {@link CurrentUserProvider} sahtesiyle verilir. Sonuçlar transaction kapandıktan sonra SQL ile
 * okunur.
 */
class RunCarryOverIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Mediator mediator;

    @Autowired
    private Clock clock;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveBalanceRepository balanceRepository;

    @Autowired
    private AnnualAccrualRunRepository runRepository;

    @MockitoBean
    private CurrentUserProvider currentUser;

    private int fromYear;
    private int toYear;
    private Department department;
    private Employee hr;
    private Employee ayse;

    @BeforeEach
    void setUp() {
        toYear = LocalDate.now(clock).getYear();
        fromYear = toYear - 1;
        department = departmentRepository.save(TestData.department());
        hr = employeeRepository.save(TestData.employee(department, Role.HR));
        when(currentUser.currentEmployee()).thenReturn(hr);

        // Ayşe geçen yıl 20 günün 8'ini kullandı; bu yılın hak edişi (T3) çalıştırıldı.
        ayse = employeeRepository.save(TestData.employee(department));
        annualBalance(ayse, fromYear, "20", "8");
        annualBalance(ayse, toYear, "20", "0");
    }

    @Test
    void carriesTheRemainingAnnualDaysToThisYear() {
        accrualRunFor(toYear);

        RunCarryOverResponse response = mediator.send(new RunCarryOverRequest(fromYear));

        assertThat(response.fromYear()).isEqualTo(fromYear);
        assertThat(response.toYear()).isEqualTo(toYear);
        assertThat(response.carriedEmployees()).isEqualTo(1);
        assertThat(response.carriedDays()).isEqualByComparingTo("12");
        assertThat(response.skippedEmployees()).isZero();

        // Eski yıl: hak edilen 20 → 8, kullanılan 8 olarak kalır, kalan 0.
        assertThat(annual(ayse, fromYear)).containsEntry("entitled_days", new BigDecimal("8.0"))
                .containsEntry("used_days", new BigDecimal("8.0"));
        // Yeni yıl: 20 + 12 = 32.
        assertThat(annual(ayse, toYear)).containsEntry("entitled_days", new BigDecimal("32.0"));

        assertThat(transactions()).containsExactly(
                List.of("DEDUCTION", "12.0", (toYear) + " yılına devredildi", fromYear),
                List.of("ACCRUAL", "12.0", fromYear + " yılından devreden", toYear));
        assertThat(jdbcTemplate.queryForList(
                "select distinct created_by from leave_mgmt.balance_transactions", Long.class))
                .containsExactly(hr.getId());
    }

    @Test
    void carriesOnlyRemainingAnnualDaysOfActiveEmployeesWithANewYearBalance() {
        accrualRunFor(toYear);
        Employee usedEverything = employeeRepository.save(TestData.employee(department));
        annualBalance(usedEverything, fromYear, "14", "14");
        annualBalance(usedEverything, toYear, "14", "0");
        Employee withoutNewYearBalance = employeeRepository.save(TestData.employee(department));
        annualBalance(withoutNewYearBalance, fromYear, "14", "4");
        Employee left = TestData.employee(department);
        left.setActive(false);
        left = employeeRepository.save(left);
        annualBalance(left, fromYear, "14", "0");
        annualBalance(left, toYear, "14", "0");
        // Mazeret izni devretmez.
        balanceRepository.save(TestData.balance(ayse, leaveType("EXCUSE"), fromYear, "5"));
        balanceRepository.save(TestData.balance(ayse, leaveType("EXCUSE"), toYear, "5"));

        RunCarryOverResponse response = mediator.send(new RunCarryOverRequest(fromYear));

        assertThat(response.carriedEmployees()).isEqualTo(1);       // yalnızca Ayşe
        assertThat(response.carriedDays()).isEqualByComparingTo("12");
        assertThat(response.skippedEmployees()).isEqualTo(1);       // yeni yıl bakiyesi olmayan
        assertThat(annual(withoutNewYearBalance, fromYear)).containsEntry("entitled_days", new BigDecimal("14.0"));
        assertThat(annual(left, toYear)).containsEntry("entitled_days", new BigDecimal("14.0"));
        assertThat(jdbcTemplate.queryForObject("""
                select b.entitled_days from leave_mgmt.leave_balances b
                join leave_mgmt.leave_types t on t.id = b.leave_type_id
                where b.employee_id = ? and t.code = 'EXCUSE' and b.year = ?
                """, BigDecimal.class, ayse.getId(), toYear)).isEqualByComparingTo("5");
        assertThat(countRows("balance_transactions")).isEqualTo(2);
    }

    @Test
    void runningItAgainCarriesNothingMore() {
        accrualRunFor(toYear);
        mediator.send(new RunCarryOverRequest(fromYear));

        RunCarryOverResponse again = mediator.send(new RunCarryOverRequest(fromYear));

        assertThat(again.carriedEmployees()).isZero();
        assertThat(again.carriedDays()).isEqualByComparingTo("0");
        assertThat(annual(ayse, toYear)).containsEntry("entitled_days", new BigDecimal("32.0"));
        assertThat(countRows("balance_transactions")).isEqualTo(2);
    }

    @Test
    void requiresThisYearsAccrualToHaveRun() {
        assertThatThrownBy(() -> mediator.send(new RunCarryOverRequest(fromYear)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(toYear + " yılının hak edişi çalıştırılmadan devir yapılamaz.")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCRUAL_NOT_RUN);
        assertThat(annual(ayse, fromYear)).containsEntry("entitled_days", new BigDecimal("20.0"));
        assertThat(countRows("balance_transactions")).isZero();
    }

    @Test
    void anInactiveHrCannotRunIt() {
        accrualRunFor(toYear);
        Employee inactiveHr = TestData.employee(department, Role.HR);
        inactiveHr.setActive(false);
        when(currentUser.currentEmployee()).thenReturn(employeeRepository.save(inactiveHr));

        assertThatThrownBy(() -> mediator.send(new RunCarryOverRequest(fromYear)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INACTIVE_EMPLOYEE);
        assertThat(countRows("balance_transactions")).isZero();
    }

    // ---- yardımcılar ----

    private void annualBalance(Employee employee, int year, String entitled, String used) {
        LeaveBalance balance = TestData.balance(employee, leaveType("ANNUAL"), year, entitled);
        if (new BigDecimal(used).signum() > 0) {
            balance.use(new BigDecimal(used));
        }
        balanceRepository.save(balance);
    }

    private void accrualRunFor(int year) {
        runRepository.save(new AnnualAccrualRun(year, OffsetDateTime.now(clock), hr, 0));
    }

    private Map<String, Object> annual(Employee employee, int year) {
        return jdbcTemplate.queryForMap("""
                select b.entitled_days, b.used_days from leave_mgmt.leave_balances b
                join leave_mgmt.leave_types t on t.id = b.leave_type_id
                where b.employee_id = ? and t.code = 'ANNUAL' and b.year = ?
                """, employee.getId(), year);
    }

    /** Hareketler eklenme sırasıyla: tür, gün, not, bakiyenin yılı. */
    private List<List<Object>> transactions() {
        return jdbcTemplate.query("""
                select x.type, x.days, x.note, b.year from leave_mgmt.balance_transactions x
                join leave_mgmt.leave_balances b on b.id = x.leave_balance_id
                order by x.id
                """, (rs, row) -> List.<Object>of(rs.getString("type"), rs.getBigDecimal("days").toPlainString(),
                rs.getString("note"), rs.getInt("year")));
    }
}
