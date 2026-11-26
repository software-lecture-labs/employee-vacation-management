package com.dogus.vacation.application.usecase.balance.accrual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.domain.policy.EntitlementPolicy;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Yıllık hak edişi (T3) mediator üzerinden, gerçek PostgreSQL ile çalıştırır.
 *
 * <p>İşlemi yapan İK {@link CurrentUserProvider} sahtesiyle verilir; uç noktanın JWT ile
 * çalışması {@code LeaveBalanceControllerIntegrationTest}'te ayrıca denenir.
 * {@link EntitlementPolicy} gerçek nesnenin casusudur (spy): normalde gerçek hesabı yapar,
 * rollback testinde 3. çalışanda hata fırlatması istenir.
 *
 * <p>Hak ediş yılı gelecek yıldır (Validator'ın kabul ettiği yıllardan biri). İşe giriş ve
 * doğum yılları bu yıla göre verilir; test hangi yıl çalışırsa çalışsın kıdem aynı çıkar.
 * Sonuçlar transaction kapandıktan sonra SQL ile okunur, böylece commit ve rollback gerçekten
 * olmuş olur.
 */
class RunAnnualAccrualIntegrationTest extends AbstractIntegrationTest {

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
    private LeaveTypeRepository leaveTypeRepository;

    @MockitoBean
    private CurrentUserProvider currentUser;

    @MockitoSpyBean
    private EntitlementPolicy entitlementPolicy;

    private int year;
    private Department department;
    private Employee hr;

    @BeforeEach
    void setUp() {
        year = LocalDate.now(clock).getYear() + 1;
        department = departmentRepository.save(TestData.department());
        hr = employee(Role.HR, year - 8, year - 40);          // 8 yıl → 20 gün
        when(currentUser.currentEmployee()).thenReturn(hr);
    }

    @Test
    void opensAnnualLeaveBySeniorityAndAgeAndExcuseByItsLimit() {
        Employee newcomer = employee(year, year - 30);        // 0 yıl → hak yok
        Employee junior = employee(year - 3, year - 30);      // 3 yıl → 14
        Employee mid = employee(year - 10, year - 35);        // 10 yıl → 20
        Employee senior = employee(year - 20, year - 45);     // 20 yıl → 26
        Employee young = employee(year - 1, year - 18);       // 14, 18 yaş → en az 20
        Employee older = employee(year - 2, year - 55);       // 14, 55 yaş → en az 20

        RunAnnualAccrualResponse response = mediator.send(new RunAnnualAccrualRequest(year));

        assertThat(response.year()).isEqualTo(year);
        assertThat(response.accruedEmployees()).isEqualTo(7);
        assertThat(response.skippedEmployees()).isZero();
        assertThat(response.openedBalances()).isEqualTo(14);
        assertThat(response.runAt()).isNotNull();

        assertThat(entitledDays(newcomer, "ANNUAL")).isEqualByComparingTo("0");
        assertThat(entitledDays(junior, "ANNUAL")).isEqualByComparingTo("14");
        assertThat(entitledDays(mid, "ANNUAL")).isEqualByComparingTo("20");
        assertThat(entitledDays(senior, "ANNUAL")).isEqualByComparingTo("26");
        assertThat(entitledDays(young, "ANNUAL")).isEqualByComparingTo("20");
        assertThat(entitledDays(older, "ANNUAL")).isEqualByComparingTo("20");
        assertThat(entitledDays(hr, "ANNUAL")).isEqualByComparingTo("20");
        assertThat(entitledDays(newcomer, "EXCUSE")).isEqualByComparingTo("5");
        assertThat(entitledDays(senior, "EXCUSE")).isEqualByComparingTo("5");

        // Hastalık ve ücretsiz izin bakiyeden düşmediği için açılmaz (BR-03).
        assertThat(openedLeaveTypeCodes()).containsExactly("ANNUAL", "EXCUSE");
    }

    @Test
    void writesAccrualTransactionsAndTheRunRecord() {
        Employee newcomer = employee(year, year - 30);        // yıllık izni 0: satır var, hareket yok
        employee(year - 3, year - 30);

        mediator.send(new RunAnnualAccrualRequest(year));

        assertThat(countRows("leave_balances")).isEqualTo(6);
        assertThat(countRows("balance_transactions")).isEqualTo(5);
        assertThat(jdbcTemplate.queryForList(
                "select distinct type from leave_mgmt.balance_transactions", String.class))
                .containsExactly("ACCRUAL");
        assertThat(jdbcTemplate.queryForList(
                "select distinct created_by from leave_mgmt.balance_transactions", Long.class))
                .containsExactly(hr.getId());
        assertThat(jdbcTemplate.queryForList(
                "select distinct note from leave_mgmt.balance_transactions", String.class))
                .containsExactly(year + " yılı hak edişi");
        assertThat(transactionCount(newcomer, "ANNUAL")).isZero();

        Map<String, Object> run = jdbcTemplate.queryForMap(
                "select year, processed_count, run_by from leave_mgmt.annual_accrual_runs");
        assertThat(((Number) run.get("year")).intValue()).isEqualTo(year);
        assertThat(((Number) run.get("processed_count")).intValue()).isEqualTo(3);
        assertThat(run.get("run_by")).isEqualTo(hr.getId());
    }

    @Test
    void skipsInactiveNotYetHiredAndAlreadyOpenedEmployees() {
        Employee inactive = TestData.employee(department);
        inactive.setActive(false);
        employeeRepository.save(inactive);
        Employee notYetHired = employee(year + 1, year - 30);
        // Yıl içinde eklenmiş gibi: T4 iki bakiyeyi de açmış.
        Employee alreadyOpened = employee(year - 5, year - 30);
        balanceRepository.save(TestData.balance(alreadyOpened, leaveType("ANNUAL"), year, "14"));
        balanceRepository.save(TestData.balance(alreadyOpened, leaveType("EXCUSE"), year, "5"));
        // Yalnızca yıllık izni açılmış: eksik olan mazeret açılır.
        Employee partlyOpened = employee(year - 5, year - 30);
        balanceRepository.save(TestData.balance(partlyOpened, leaveType("ANNUAL"), year, "14"));

        RunAnnualAccrualResponse response = mediator.send(new RunAnnualAccrualRequest(year));

        assertThat(response.accruedEmployees()).isEqualTo(2);       // hr ve partlyOpened
        assertThat(response.skippedEmployees()).isEqualTo(2);       // notYetHired ve alreadyOpened
        assertThat(response.openedBalances()).isEqualTo(3);
        assertThat(balanceCount(inactive)).isZero();
        assertThat(balanceCount(notYetHired)).isZero();
        assertThat(balanceCount(alreadyOpened)).isEqualTo(2);
        assertThat(entitledDays(partlyOpened, "EXCUSE")).isEqualByComparingTo("5");
        assertThat(transactionCount(alreadyOpened, "ANNUAL")).isZero();
    }

    @Test
    void opensOtherDeductingTypesOnlyWhenTheyAreActiveAndHaveAYearlyLimit() {
        leaveTypeRepository.save(new LeaveType("MARRIAGE", "Evlilik izni", true, new BigDecimal("3")));
        leaveTypeRepository.save(new LeaveType("STUDY", "Eğitim izni", true, null));
        LeaveType retired = new LeaveType("RETIRED", "Kaldırılmış izin", true, new BigDecimal("2"));
        retired.setActive(false);
        leaveTypeRepository.save(retired);

        mediator.send(new RunAnnualAccrualRequest(year));

        assertThat(openedLeaveTypeCodes()).containsExactly("ANNUAL", "EXCUSE", "MARRIAGE");
        assertThat(entitledDays(hr, "MARRIAGE")).isEqualByComparingTo("3");
    }

    @Test
    void aSecondRunForTheSameYearIsRejectedAndChangesNothing() {
        employee(year - 3, year - 30);
        mediator.send(new RunAnnualAccrualRequest(year));
        employee(year - 3, year - 30);                        // ikinci çalıştırmadan önce eklenen çalışan

        assertThatThrownBy(() -> mediator.send(new RunAnnualAccrualRequest(year)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(year + " yılının hak edişi zaten çalıştırıldı.")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCRUAL_ALREADY_RUN);
        assertThat(countRows("annual_accrual_runs")).isEqualTo(1);
        assertThat(countRows("leave_balances")).isEqualTo(4);
        assertThat(countRows("balance_transactions")).isEqualTo(4);
    }

    @Test
    void anInactiveHrCannotRunIt() {
        Employee inactiveHr = TestData.employee(department, Role.HR);
        inactiveHr.setActive(false);
        when(currentUser.currentEmployee()).thenReturn(employeeRepository.save(inactiveHr));

        assertThatThrownBy(() -> mediator.send(new RunAnnualAccrualRequest(year)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INACTIVE_EMPLOYEE);
        assertThat(countRows("leave_balances")).isZero();
        assertThat(countRows("annual_accrual_runs")).isZero();
    }

    /**
     * T3'ün kanıtı: 4 çalışandan 3'üncüsünde hata çıkınca ilk ikisine açılan bakiyeler ve
     * hareketler de geri alınır, çalıştırma kaydı hiç yazılmaz. Kimlikler veritabanında
     * üretildiği için ({@code IDENTITY}) ilk iki çalışanın satırları hata anında veritabanına
     * gönderilmiştir; sıfır kalmaları rollback'in gerçekten olduğunu gösterir. Kayıt geri
     * alındığı için yıl kilitlenmez: hata giderilince aynı yıl yeniden çalıştırılabilir.
     */
    @Test
    void aFailureHalfwayRollsBackEverythingAndTheYearCanBeRunAgain() {
        employee(year - 3, year - 30);
        employee(year - 10, year - 30);
        employee(year - 20, year - 30);
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 3) {
                throw new IllegalStateException("Test için 3. çalışanda hata");
            }
            return invocation.callRealMethod();
        }).when(entitlementPolicy).annualLeaveDays(any(), any(), anyInt());

        assertThatThrownBy(() -> mediator.send(new RunAnnualAccrualRequest(year)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test için 3. çalışanda hata");
        verify(entitlementPolicy, times(3)).annualLeaveDays(any(), any(), anyInt());
        assertThat(countRows("leave_balances")).isZero();
        assertThat(countRows("balance_transactions")).isZero();
        assertThat(countRows("annual_accrual_runs")).isZero();

        doCallRealMethod().when(entitlementPolicy).annualLeaveDays(any(), any(), anyInt());
        RunAnnualAccrualResponse retry = mediator.send(new RunAnnualAccrualRequest(year));

        assertThat(retry.accruedEmployees()).isEqualTo(4);
        assertThat(countRows("leave_balances")).isEqualTo(8);
        assertThat(countRows("annual_accrual_runs")).isEqualTo(1);
    }

    // ---- yardımcılar ----

    private Employee employee(int hireYear, int birthYear) {
        return employee(Role.EMPLOYEE, hireYear, birthYear);
    }

    private Employee employee(Role role, int hireYear, int birthYear) {
        Employee employee = TestData.employee(department, role);
        employee.setHireDate(LocalDate.of(hireYear, 3, 1));
        employee.setBirthDate(LocalDate.of(birthYear, 6, 1));
        return employeeRepository.save(employee);
    }

    private BigDecimal entitledDays(Employee employee, String leaveTypeCode) {
        return jdbcTemplate.queryForObject("""
                select b.entitled_days from leave_mgmt.leave_balances b
                join leave_mgmt.leave_types t on t.id = b.leave_type_id
                where b.employee_id = ? and t.code = ? and b.year = ?
                """, BigDecimal.class, employee.getId(), leaveTypeCode, year);
    }

    private int balanceCount(Employee employee) {
        return jdbcTemplate.queryForObject(
                "select count(*) from leave_mgmt.leave_balances where employee_id = ? and year = ?",
                Integer.class, employee.getId(), year);
    }

    private int transactionCount(Employee employee, String leaveTypeCode) {
        return jdbcTemplate.queryForObject("""
                select count(*) from leave_mgmt.balance_transactions x
                join leave_mgmt.leave_balances b on b.id = x.leave_balance_id
                join leave_mgmt.leave_types t on t.id = b.leave_type_id
                where b.employee_id = ? and t.code = ? and b.year = ?
                """, Integer.class, employee.getId(), leaveTypeCode, year);
    }

    private List<String> openedLeaveTypeCodes() {
        return jdbcTemplate.queryForList("""
                select distinct t.code, t.id from leave_mgmt.leave_balances b
                join leave_mgmt.leave_types t on t.id = b.leave_type_id
                order by t.id
                """).stream().map(row -> (String) row.get("code")).toList();
    }
}
