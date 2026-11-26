package com.dogus.vacation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.InsufficientBalanceException;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link LeaveBalanceService}'i gerçek PostgreSQL üzerinde doğrular: bakiye satırı ve
 * hareket kaydı gerçekten yazılıyor mu, hata olunca ikisi de geri alınıyor mu.
 *
 * <p>Servis transaction açmadığı için her çağrı, Handler'ın yapacağı gibi bir
 * {@link TransactionTemplate} içinde yapılır. Sonuç, transaction kapandıktan sonra
 * veritabanından okunur; böylece commit veya rollback gerçekten olmuş olur.
 */
class LeaveBalanceServiceIntegrationTest extends AbstractIntegrationTest {

    private static final int YEAR = 2026;

    @Autowired
    private LeaveBalanceService service;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveBalanceRepository balanceRepository;

    @Autowired
    private LeaveRequestRepository requestRepository;

    private Employee employee;
    private Employee hr;

    @BeforeEach
    void createEmployees() {
        Department department = departmentRepository.save(TestData.department());
        employee = employeeRepository.save(TestData.employee(department));
        hr = employeeRepository.save(TestData.employee(department, Role.HR));
    }

    // ---- openBalance (T3, T4) ----

    @Test
    void openBalanceSavesTheBalanceAndAnAccrualTransaction() {
        LeaveBalance balance = inTransactionReturning(() ->
                service.openBalance(employee, leaveType("ANNUAL"), YEAR, new BigDecimal("14"), hr));

        assertThat(usedDays(balance)).isEqualByComparingTo("0");
        assertThat(countRows("balance_transactions")).isEqualTo(1);
        Map<String, Object> transaction = lastTransaction();
        assertThat(transaction.get("type")).isEqualTo("ACCRUAL");
        assertThat((BigDecimal) transaction.get("days")).isEqualByComparingTo("14");
        assertThat(transaction.get("leave_request_id")).isNull();
        assertThat(transaction.get("note")).isEqualTo("2026 yılı hak edişi");
        assertThat(transaction.get("created_by")).isEqualTo(hr.getId());
    }

    @Test
    void openBalanceWithZeroDaysWritesNoTransaction() {
        inTransaction(() -> service.openBalance(employee, leaveType("ANNUAL"), YEAR, BigDecimal.ZERO, hr));

        assertThat(countRows("leave_balances")).isEqualTo(1);
        assertThat(countRows("balance_transactions")).isZero();
    }

    @Test
    void openBalanceRejectsASecondBalanceForTheSameYear() {
        inTransaction(() -> service.openBalance(employee, leaveType("ANNUAL"), YEAR, new BigDecimal("14"), hr));

        assertThatThrownBy(() -> inTransaction(() ->
                service.openBalance(employee, leaveType("ANNUAL"), YEAR, new BigDecimal("20"), hr)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VALUE);
        assertThat(countRows("leave_balances")).isEqualTo(1);
        assertThat(countRows("balance_transactions")).isEqualTo(1);
    }

    // ---- deduct (T1) ----

    @Test
    void deductIncreasesUsedDaysAndRecordsADeduction() {
        LeaveBalance balance = annualBalance("14");
        LeaveRequest request = annualRequest("3");

        inTransaction(() -> service.deduct(request, hr));

        assertThat(usedDays(balance)).isEqualByComparingTo("3");
        Map<String, Object> transaction = lastTransaction();
        assertThat(transaction.get("type")).isEqualTo("DEDUCTION");
        assertThat((BigDecimal) transaction.get("days")).isEqualByComparingTo("3");
        assertThat(transaction.get("leave_request_id")).isEqualTo(request.getId());
        assertThat(transaction.get("note")).isEqualTo("İzin talebi #" + request.getId() + " onaylandı");
        assertThat(transaction.get("created_by")).isEqualTo(hr.getId());
    }

    @Test
    void deductRollsBackWhenTheBalanceIsInsufficient() {
        LeaveBalance balance = annualBalance("2");
        LeaveRequest request = annualRequest("3");

        assertThatThrownBy(() -> inTransaction(() -> service.deduct(request, hr)))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Yetersiz izin bakiyesi: talep 3 gün, kalan 2 gün.");
        assertThat(usedDays(balance)).isEqualByComparingTo("0");
        assertThat(countRows("balance_transactions")).isZero();
    }

    @Test
    void deductFailsWhenThereIsNoBalanceForThatYear() {
        LeaveRequest request = annualRequest("3");

        assertThatThrownBy(() -> inTransaction(() -> service.deduct(request, hr)))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEAVE_BALANCE_NOT_FOUND);
    }

    @Test
    void deductDoesNothingForLeaveTypesThatDoNotUseTheBalance() {
        LeaveRequest sickLeave = requestRepository.save(TestData.leaveRequest(employee, leaveType("SICK"),
                LocalDate.of(YEAR, 12, 1), LocalDate.of(YEAR, 12, 3), "3"));

        inTransaction(() -> service.deduct(sickLeave, hr));

        assertThat(countRows("balance_transactions")).isZero();
    }

    // ---- refund (T2) ----

    @Test
    void refundGivesTheDaysBackAndRecordsARefund() {
        LeaveBalance balance = annualBalance("14");
        LeaveRequest request = annualRequest("3");
        inTransaction(() -> service.deduct(request, hr));

        inTransaction(() -> service.refund(request, hr));

        assertThat(usedDays(balance)).isEqualByComparingTo("0");
        assertThat(countRows("balance_transactions")).isEqualTo(2);
        Map<String, Object> transaction = lastTransaction();
        assertThat(transaction.get("type")).isEqualTo("REFUND");
        assertThat((BigDecimal) transaction.get("days")).isEqualByComparingTo("3");
        assertThat(transaction.get("note")).isEqualTo("İzin talebi #" + request.getId() + " iptal edildi");
    }

    // ---- checkSufficient (talep açma) ----

    @Test
    void checkSufficientPassesOrFailsWithoutWritingAnything() {
        LeaveBalance balance = annualBalance("5");

        inTransaction(() -> service.checkSufficient(employee, leaveType("ANNUAL"), YEAR, new BigDecimal("5")));
        assertThatThrownBy(() -> inTransaction(() ->
                service.checkSufficient(employee, leaveType("ANNUAL"), YEAR, new BigDecimal("6"))))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(usedDays(balance)).isEqualByComparingTo("0");
        assertThat(countRows("balance_transactions")).isZero();
    }

    @Test
    void checkSufficientIgnoresLeaveTypesThatDoNotUseTheBalance() {
        inTransaction(() -> service.checkSufficient(employee, leaveType("UNPAID"), YEAR, new BigDecimal("10")));
    }

    // ---- transaction kuralı ----

    @Test
    void refusesToRunOutsideATransaction() {
        LeaveRequest request = annualRequest("3");

        assertThatThrownBy(() -> service.deduct(request, hr))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    // ---- yardımcılar ----

    private LeaveBalance annualBalance(String entitledDays) {
        return balanceRepository.save(TestData.balance(employee, leaveType("ANNUAL"), YEAR, entitledDays));
    }

    private LeaveRequest annualRequest(String workingDays) {
        return requestRepository.save(TestData.leaveRequest(employee, leaveType("ANNUAL"),
                LocalDate.of(YEAR, 12, 1), LocalDate.of(YEAR, 12, 3), workingDays));
    }

    private <T> T inTransactionReturning(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    private void inTransaction(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }

    private BigDecimal usedDays(LeaveBalance balance) {
        return jdbcTemplate.queryForObject(
                "select used_days from leave_mgmt.leave_balances where id = ?", BigDecimal.class, balance.getId());
    }

    private Map<String, Object> lastTransaction() {
        return jdbcTemplate.queryForMap("""
                select type, days, leave_request_id, note, created_by
                from leave_mgmt.balance_transactions
                order by id desc
                limit 1
                """);
    }
}
