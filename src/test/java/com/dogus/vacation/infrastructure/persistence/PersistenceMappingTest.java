package com.dogus.vacation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.model.AnnualAccrualRun;
import com.dogus.vacation.domain.model.BalanceTransaction;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.model.PublicHoliday;
import com.dogus.vacation.domain.model.enums.BalanceTransactionType;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Entity'lerin test veritabanına (db/scripts ile kurulan PostgreSQL) bağlandığını doğrular.
 *
 * <p>Uygulamanın açılması, {@code ddl-auto: validate} sayesinde 9 entity'nin tablolarla
 * uyumlu olduğunu zaten kanıtlar; bu testler ayrıca seed verisini, auditing'i,
 * ilişkili kayıtların kaydedilmesini ve alanların doğru kolona yazılıp geri okunmasını
 * dener. Çalışması için Docker açık olmalıdır.
 */
class PersistenceMappingTest extends AbstractIntegrationTest {

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private BalanceTransactionRepository transactionRepository;

    @Autowired
    private AnnualAccrualRunRepository accrualRunRepository;

    @Autowired
    private PublicHolidayRepository holidayRepository;

    @Test
    void seedScriptLoadsFourLeaveTypes() {
        assertThat(leaveTypeRepository.findAll())
                .extracting(LeaveType::getCode)
                .containsExactlyInAnyOrder("ANNUAL", "EXCUSE", "SICK", "UNPAID");
    }

    @Test
    void savingEntityFillsIdAndTimestamps() {
        Department saved = departmentRepository.save(TestData.department());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void balanceIsSavedWithItsEmployeeAndRemainingDaysAreCalculated() {
        Department department = departmentRepository.save(TestData.department());
        Employee employee = employeeRepository.save(TestData.employee(department, Role.EMPLOYEE));
        LeaveBalance saved = leaveBalanceRepository.save(
                TestData.balance(employee, leaveType("ANNUAL"), 2026, "14"));

        LeaveBalance reloaded = leaveBalanceRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getYear()).isEqualTo(2026);
        assertThat(reloaded.remainingDays()).isEqualByComparingTo("14");
        assertThat(countRows("leave_balances")).isEqualTo(1);
    }

    // ---- kaydedilip geri okunan alanlar: her alan doğru kolona yazılıp okunuyor mu ----

    @Test
    void employeeAndDepartmentChangesAreSavedAndReadBack() {
        Department first = departmentRepository.save(TestData.department());
        Department second = departmentRepository.save(TestData.department());
        Employee employee = employeeRepository.save(TestData.employee(first, Role.EMPLOYEE));

        employee.setFirstName("Ayşe");
        employee.setLastName("Yılmaz");
        employee.setRole(Role.MANAGER);
        employee.setDepartment(second);
        employeeRepository.save(employee);
        second.setName("İnsan Kaynakları");
        departmentRepository.save(second);

        Employee reloaded = employeeRepository.findById(employee.getId()).orElseThrow();
        assertThat(reloaded.getFirstName()).isEqualTo("Ayşe");
        assertThat(reloaded.getLastName()).isEqualTo("Yılmaz");
        assertThat(reloaded.getRole()).isEqualTo(Role.MANAGER);
        assertThat(reloaded.getDepartment().getId()).isEqualTo(second.getId());
        assertThat(departmentRepository.findById(second.getId()).orElseThrow().getName())
                .isEqualTo("İnsan Kaynakları");
    }

    @Test
    void balanceTransactionIsSavedAndReadBack() {
        Department department = departmentRepository.save(TestData.department());
        Employee hr = employeeRepository.save(TestData.employee(department, Role.HR));
        LeaveBalance balance = leaveBalanceRepository.save(
                TestData.balance(hr, leaveType("ANNUAL"), 2026, "20"));
        BalanceTransaction saved = transactionRepository.save(new BalanceTransaction(
                balance, null, BalanceTransactionType.ACCRUAL, new BigDecimal("20"), "2026 yılı hak edişi", hr));

        BalanceTransaction reloaded = transactionRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getLeaveBalance().getId()).isEqualTo(balance.getId());
        assertThat(reloaded.getLeaveRequest()).isNull();
        assertThat(reloaded.getType()).isEqualTo(BalanceTransactionType.ACCRUAL);
        assertThat(reloaded.getDays()).isEqualByComparingTo("20");
        assertThat(reloaded.getNote()).isEqualTo("2026 yılı hak edişi");
        assertThat(reloaded.getCreatedBy().getId()).isEqualTo(hr.getId());
    }

    @Test
    void annualAccrualRunIsSavedAndReadBack() {
        Department department = departmentRepository.save(TestData.department());
        Employee hr = employeeRepository.save(TestData.employee(department, Role.HR));
        // PostgreSQL zamanı mikro saniyeye kadar saklar.
        OffsetDateTime runAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        AnnualAccrualRun saved = accrualRunRepository.save(new AnnualAccrualRun(2027, runAt, hr, 148));

        AnnualAccrualRun reloaded = accrualRunRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getYear()).isEqualTo(2027);
        assertThat(reloaded.getRunAt()).isAtSameInstantAs(runAt);
        assertThat(reloaded.getRunBy().getId()).isEqualTo(hr.getId());
        assertThat(reloaded.getProcessedCount()).isEqualTo(148);
    }

    @Test
    void publicHolidayIsSavedAndReadBack() {
        PublicHoliday holiday = new PublicHoliday(LocalDate.of(2026, 10, 28), "Cumhuriyet Bayramı arifesi");
        holiday.setHolidayDate(LocalDate.of(2026, 10, 29));
        holiday.setName("Cumhuriyet Bayramı");
        PublicHoliday saved = holidayRepository.save(holiday);

        PublicHoliday reloaded = holidayRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getHolidayDate()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(reloaded.getName()).isEqualTo("Cumhuriyet Bayramı");
    }
}
