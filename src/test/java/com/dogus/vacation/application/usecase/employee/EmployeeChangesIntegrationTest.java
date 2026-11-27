package com.dogus.vacation.application.usecase.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.application.usecase.employee.deactivate.DeactivateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.deactivate.DeactivateEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.update.UpdateEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.update.UpdateEmployeeResponse;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Çalışan güncelleme (FR-07) ve pasife almayı (FR-08) mediator üzerinden, gerçek PostgreSQL ile
 * çalıştırır. BR-13 (en az 2 aktif İK) ve yönetici döngüsü kuralları burada sınanır.
 *
 * <p>Kurulum: A departmanında iki İK, bir yönetici ve ona bağlı bir çalışan; ayrıca boş bir
 * B departmanı.
 */
class EmployeeChangesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Mediator mediator;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private Clock clock;

    private LocalDate today;
    private Department departmentA;
    private Department departmentB;
    private Employee hr1;
    private Employee hr2;
    private Employee manager;
    private Employee member;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(clock);
        departmentA = departmentRepository.save(TestData.department());
        departmentB = departmentRepository.save(TestData.department());
        hr1 = employeeRepository.save(TestData.employee(departmentA, Role.HR));
        hr2 = employeeRepository.save(TestData.employee(departmentA, Role.HR));
        manager = employeeRepository.save(TestData.employee(departmentA, Role.MANAGER));
        Employee newMember = TestData.employee(departmentA, Role.EMPLOYEE);
        newMember.setManager(manager);
        member = employeeRepository.save(newMember);
    }

    /** Çalışanın mevcut bilgileriyle bir güncelleme isteği; testler yalnızca değişen alanı verir. */
    private UpdateEmployeeRequest unchanged(Employee employee) {
        return new UpdateEmployeeRequest(employee.getId(), employee.getFirstName(), employee.getLastName(),
                employee.getEmail(), employee.getHireDate(), employee.getBirthDate(), employee.getRole(),
                departmentA.getId(), employee.getManager() == null ? null : employee.getManager().getId());
    }

    private UpdateEmployeeRequest withRole(Employee employee, Role role) {
        UpdateEmployeeRequest r = unchanged(employee);
        return new UpdateEmployeeRequest(r.id(), r.firstName(), r.lastName(), r.email(), r.hireDate(),
                r.birthDate(), role, r.departmentId(), r.managerId());
    }

    private UpdateEmployeeRequest withManager(Employee employee, Long managerId) {
        UpdateEmployeeRequest r = unchanged(employee);
        return new UpdateEmployeeRequest(r.id(), r.firstName(), r.lastName(), r.email(), r.hireDate(),
                r.birthDate(), r.role(), r.departmentId(), managerId);
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    // ---- UpdateEmployee ----

    @Test
    void updatesTheEmployeesInformation() {
        UpdateEmployeeResponse response = mediator.send(new UpdateEmployeeRequest(member.getId(), " Elif ",
                " Şahin ", "elif.sahin@test.local", today.minusYears(4), today.minusYears(27), Role.MANAGER,
                departmentB.getId(), null));

        assertThat(response.employee().firstName()).isEqualTo("Elif");
        assertThat(response.employee().lastName()).isEqualTo("Şahin");
        assertThat(response.employee().email()).isEqualTo("elif.sahin@test.local");
        assertThat(response.employee().role()).isEqualTo(Role.MANAGER);
        assertThat(response.employee().departmentId()).isEqualTo(departmentB.getId());
        assertThat(response.employee().manager()).isNull();
        Employee saved = employeeRepository.findById(member.getId()).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("elif.sahin@test.local");
        assertThat(saved.getPasswordHash()).isEqualTo(TestData.DEFAULT_PASSWORD_HASH);
    }

    @Test
    void keepingTheSameEmailIsAllowed() {
        UpdateEmployeeResponse response = mediator.send(unchanged(member));

        assertThat(response.employee().email()).isEqualTo(member.getEmail());
    }

    @Test
    void rejectsAnEmailUsedByAnotherEmployee() {
        UpdateEmployeeRequest r = unchanged(member);
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(r.id(), r.firstName(), r.lastName(),
                manager.getEmail().toUpperCase(), r.hireDate(), r.birthDate(), r.role(), r.departmentId(),
                r.managerId());

        assertThatThrownBy(() -> mediator.send(request))
                .isInstanceOf(BusinessException.class)
                .extracting(EmployeeChangesIntegrationTest::errorCodeOf)
                .isEqualTo(ErrorCode.DUPLICATE_VALUE);
    }

    @Test
    void anEmployeeCannotBeTheirOwnManager() {
        assertThatThrownBy(() -> mediator.send(withManager(member, member.getId())))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("managerId"));
    }

    @Test
    void aManagerCannotReportToSomeoneInTheirOwnTeam() {
        assertThatThrownBy(() -> mediator.send(withManager(manager, member.getId())))
                .isInstanceOf(BusinessException.class)
                .extracting(EmployeeChangesIntegrationTest::errorCodeOf)
                .isEqualTo(ErrorCode.MANAGER_CYCLE);
        assertThat(employeeRepository.findById(manager.getId()).orElseThrow().getManager()).isNull();
    }

    @Test
    void theRoleOfOneOfTheLastTwoActiveHrCannotChange() {
        assertThatThrownBy(() -> mediator.send(withRole(hr1, Role.EMPLOYEE)))
                .isInstanceOf(BusinessException.class)
                .extracting(EmployeeChangesIntegrationTest::errorCodeOf)
                .isEqualTo(ErrorCode.MINIMUM_ACTIVE_HR_REQUIRED);
        assertThat(employeeRepository.findById(hr1.getId()).orElseThrow().getRole()).isEqualTo(Role.HR);
    }

    @Test
    void anHrRoleCanChangeWhenThreeActiveHrExist() {
        employeeRepository.save(TestData.employee(departmentA, Role.HR));

        UpdateEmployeeResponse response = mediator.send(withRole(hr1, Role.MANAGER));

        assertThat(response.employee().role()).isEqualTo(Role.MANAGER);
    }

    @Test
    void updatingAMissingEmployeeIsNotFound() {
        UpdateEmployeeRequest r = unchanged(member);
        assertThatThrownBy(() -> mediator.send(r.withId(999_999L)))
                .isInstanceOf(NotFoundException.class)
                .extracting(EmployeeChangesIntegrationTest::errorCodeOf)
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    // ---- DeactivateEmployee ----

    @Test
    void deactivatesAnEmployee() {
        DeactivateEmployeeResponse response = mediator.send(new DeactivateEmployeeRequest(member.getId()));

        assertThat(response.employee().active()).isFalse();
        assertThat(employeeRepository.findById(member.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void deactivatingTwiceChangesNothing() {
        mediator.send(new DeactivateEmployeeRequest(member.getId()));

        DeactivateEmployeeResponse again = mediator.send(new DeactivateEmployeeRequest(member.getId()));

        assertThat(again.employee().active()).isFalse();
    }

    @Test
    void oneOfTheLastTwoActiveHrCannotBeDeactivated() {
        assertThatThrownBy(() -> mediator.send(new DeactivateEmployeeRequest(hr2.getId())))
                .isInstanceOf(BusinessException.class)
                .extracting(EmployeeChangesIntegrationTest::errorCodeOf)
                .isEqualTo(ErrorCode.MINIMUM_ACTIVE_HR_REQUIRED);
        assertThat(employeeRepository.findById(hr2.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void anHrCanBeDeactivatedWhenThreeActiveHrExist() {
        employeeRepository.save(TestData.employee(departmentA, Role.HR));

        DeactivateEmployeeResponse response = mediator.send(new DeactivateEmployeeRequest(hr2.getId()));

        assertThat(response.employee().active()).isFalse();
    }

    @Test
    void deactivatingAMissingEmployeeIsNotFound() {
        assertThatThrownBy(() -> mediator.send(new DeactivateEmployeeRequest(999_999L)))
                .isInstanceOf(NotFoundException.class);
    }
}
