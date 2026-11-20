package com.dogus.vacation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link LeaveHistoryService}'in gerçek PostgreSQL'e yazdığını ve BR-16'nın iki yönünü
 * doğrular: geçmiş, durum değişikliğiyle aynı transaction'da yazılır; transaction geri
 * alınırsa geçmiş satırı da kalmaz.
 */
class LeaveHistoryServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LeaveHistoryService service;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository requestRepository;

    private Employee hr;
    private LeaveRequest request;

    @BeforeEach
    void createRequest() {
        Department department = departmentRepository.save(TestData.department());
        Employee employee = employeeRepository.save(TestData.employee(department));
        hr = employeeRepository.save(TestData.employee(department, Role.HR));
        request = requestRepository.save(TestData.leaveRequest(employee, leaveType("ANNUAL"),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), "3"));
    }

    @Test
    void writesTheHistoryRow() {
        transactionTemplate.executeWithoutResult(status -> {
            request.withdraw();
            service.record(request, LeaveRequestStatus.PENDING, hr, "Planım değişti");
        });

        assertThat(countRows("leave_request_history")).isEqualTo(1);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                select leave_request_id, from_status, to_status, actor_id, note, occurred_at
                from leave_mgmt.leave_request_history
                """);
        assertThat(row.get("leave_request_id")).isEqualTo(request.getId());
        assertThat(row.get("from_status")).isEqualTo("PENDING");
        assertThat(row.get("to_status")).isEqualTo("WITHDRAWN");
        assertThat(row.get("actor_id")).isEqualTo(hr.getId());
        assertThat(row.get("note")).isEqualTo("Planım değişti");
        assertThat(row.get("occurred_at")).isNotNull();
    }

    @Test
    void insertsImmediatelyInsideTheTransaction() {
        Integer rowsBeforeCommit = transactionTemplate.execute(status -> {
            service.record(request, null, hr, null);
            return countRows("leave_request_history");
        });

        assertThat(rowsBeforeCommit).isEqualTo(1);
    }

    @Test
    void historyIsRolledBackWithTheSurroundingTransaction() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            request.withdraw();
            service.record(request, LeaveRequestStatus.PENDING, hr, null);
            throw new IllegalStateException("Sonraki adım başarısız oldu");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countRows("leave_request_history")).isZero();
    }

    @Test
    void refusesToRunOutsideATransaction() {
        assertThatThrownBy(() -> service.record(request, null, hr, null))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(countRows("leave_request_history")).isZero();
    }
}
