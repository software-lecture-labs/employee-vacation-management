package com.dogus.vacation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Entity'lerin test veritabanına (db/scripts ile kurulan PostgreSQL) bağlandığını doğrular.
 *
 * <p>Uygulamanın açılması, {@code ddl-auto: validate} sayesinde 9 entity'nin tablolarla
 * uyumlu olduğunu zaten kanıtlar; bu testler ayrıca seed verisini, auditing'i ve
 * ilişkili kayıtların kaydedilmesini dener. Çalışması için Docker açık olmalıdır.
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
}
