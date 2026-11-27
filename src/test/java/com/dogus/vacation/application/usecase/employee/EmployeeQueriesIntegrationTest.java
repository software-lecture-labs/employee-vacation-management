package com.dogus.vacation.application.usecase.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.common.view.EmployeeRef;
import com.dogus.vacation.application.common.view.EmployeeView;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.application.usecase.employee.get.GetEmployeeRequest;
import com.dogus.vacation.application.usecase.employee.get.GetEmployeeResponse;
import com.dogus.vacation.application.usecase.employee.list.ListEmployeesRequest;
import com.dogus.vacation.application.usecase.employee.list.ListEmployeesResponse;
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
 * Çalışan listesi ve detayını (FR-09) mediator üzerinden, gerçek PostgreSQL ile çalıştırır.
 *
 * <p>Kurulum: Finans'ta yönetici Ayşe Kaya ve ona bağlı Can Demir; Satış'ta Ece Arslan ve
 * pasife alınmış Ali Yılmaz.
 */
class EmployeeQueriesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Mediator mediator;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Department finance;
    private Department sales;
    private Employee manager;
    private Employee teamMember;

    @BeforeEach
    void setUp() {
        finance = departmentRepository.save(new Department("Finans"));
        sales = departmentRepository.save(new Department("Satış"));
        manager = employeeRepository.save(named(TestData.employee(finance, Role.MANAGER), "Ayşe", "Kaya"));
        Employee member = named(TestData.employee(finance, Role.EMPLOYEE), "Can", "Demir");
        member.setManager(manager);
        teamMember = employeeRepository.save(member);
        employeeRepository.save(named(TestData.employee(sales, Role.EMPLOYEE), "Ece", "Arslan"));
        Employee inactive = named(TestData.employee(sales, Role.EMPLOYEE), "Ali", "Yılmaz");
        inactive.setActive(false);
        employeeRepository.save(inactive);
    }

    private static Employee named(Employee employee, String firstName, String lastName) {
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        return employee;
    }

    private List<String> lastNames(ListEmployeesResponse response) {
        return response.employees().content().stream().map(EmployeeView::lastName).toList();
    }

    // ---- ListEmployees ----

    @Test
    void listsEveryoneSortedByLastNameOnTheDefaultPage() {
        ListEmployeesResponse response = mediator.send(new ListEmployeesRequest(null, null, null, null));

        assertThat(lastNames(response)).containsExactly("Arslan", "Demir", "Kaya", "Yılmaz");
        assertThat(response.employees().size()).isEqualTo(20);
        assertThat(response.employees().totalElements()).isEqualTo(4);
    }

    @Test
    void filtersByDepartment() {
        ListEmployeesResponse response = mediator.send(new ListEmployeesRequest(finance.getId(), null, 0, 20));

        assertThat(lastNames(response)).containsExactly("Demir", "Kaya");
    }

    @Test
    void filtersByActivity() {
        assertThat(lastNames(mediator.send(new ListEmployeesRequest(null, true, 0, 20))))
                .containsExactly("Arslan", "Demir", "Kaya");
        assertThat(lastNames(mediator.send(new ListEmployeesRequest(null, false, 0, 20))))
                .containsExactly("Yılmaz");
    }

    @Test
    void combinesTheFilters() {
        ListEmployeesResponse response = mediator.send(new ListEmployeesRequest(sales.getId(), true, 0, 20));

        assertThat(lastNames(response)).containsExactly("Arslan");
    }

    @Test
    void returnsTheRequestedPage() {
        ListEmployeesResponse secondPage = mediator.send(new ListEmployeesRequest(null, null, 1, 3));

        assertThat(lastNames(secondPage)).containsExactly("Yılmaz");
        assertThat(secondPage.employees().totalPages()).isEqualTo(2);
    }

    @Test
    void showsTheDepartmentAndTheManagerWithoutThePassword() {
        ListEmployeesResponse response = mediator.send(new ListEmployeesRequest(finance.getId(), null, 0, 20));

        EmployeeView view = response.employees().content().get(0);
        assertThat(view.firstName()).isEqualTo("Can");
        assertThat(view.departmentName()).isEqualTo("Finans");
        assertThat(view.manager()).isEqualTo(new EmployeeRef(manager.getId(), "Ayşe Kaya"));
    }

    @Test
    void rejectsInvalidFiltersAndPaging() {
        assertThatThrownBy(() -> mediator.send(new ListEmployeesRequest(0L, null, -1, 101)))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("departmentId", "page", "size"));
    }

    // ---- GetEmployee ----

    @Test
    void getReturnsTheEmployee() {
        GetEmployeeResponse response = mediator.send(new GetEmployeeRequest(teamMember.getId()));

        assertThat(response.employee().id()).isEqualTo(teamMember.getId());
        assertThat(response.employee().email()).isEqualTo(teamMember.getEmail());
        assertThat(response.employee().role()).isEqualTo(Role.EMPLOYEE);
        assertThat(response.employee().manager().fullName()).isEqualTo("Ayşe Kaya");
    }

    @Test
    void getOfAMissingEmployeeIsNotFound() {
        assertThatThrownBy(() -> mediator.send(new GetEmployeeRequest(999_999L)))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }
}
