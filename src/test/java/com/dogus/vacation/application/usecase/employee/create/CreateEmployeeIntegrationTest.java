package com.dogus.vacation.application.usecase.employee.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.common.view.BalanceView;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.domain.policy.EntitlementPolicy;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Çalışan eklemeyi (FR-06) ve ilk bakiyeleri (T4) mediator üzerinden, gerçek PostgreSQL ile
 * çalıştırır.
 *
 * <p>İşlemi yapan İK {@link CurrentUserProvider} sahtesiyle verilir. {@link EntitlementPolicy}
 * gerçek davranır; T4 rollback testinde hak hesabında hata fırlatması istenir. Sonuçlar
 * transaction kapandıktan sonra SQL ile okunur, böylece commit ve rollback gerçekten ölçülür.
 */
class CreateEmployeeIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "IlkSifre1!";

    @Autowired
    private Mediator mediator;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @MockitoBean
    private CurrentUserProvider currentUser;

    @MockitoSpyBean
    private EntitlementPolicy entitlementPolicy;

    private LocalDate today;
    private Department department;
    private Employee hr;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(clock);
        department = departmentRepository.save(TestData.department());
        hr = employeeRepository.save(TestData.employee(department, Role.HR));
        when(currentUser.currentEmployee()).thenReturn(hr);
    }

    private CreateEmployeeRequest request(String email, LocalDate hireDate, LocalDate birthDate, Long managerId) {
        return new CreateEmployeeRequest(" Deniz ", " Yıldız ", email, PASSWORD, hireDate, birthDate,
                Role.EMPLOYEE, department.getId(), managerId);
    }

    private CreateEmployeeRequest threeYearsSeniority(String email) {
        return request(email, today.minusYears(3), today.minusYears(30), null);
    }

    private BigDecimal entitledDays(Long employeeId, String leaveTypeCode) {
        return jdbcTemplate.queryForObject("""
                select b.entitled_days from leave_mgmt.leave_balances b
                join leave_mgmt.leave_types t on t.id = b.leave_type_id
                where b.employee_id = ? and t.code = ? and b.year = ?""",
                BigDecimal.class, employeeId, leaveTypeCode, today.getYear());
    }

    @Test
    void savesTheEmployeeWithAHashedPassword() {
        CreateEmployeeResponse response = mediator.send(threeYearsSeniority("deniz.yildiz@test.local"));

        assertThat(response.employee().id()).isNotNull();
        assertThat(response.employee().firstName()).isEqualTo("Deniz");
        assertThat(response.employee().lastName()).isEqualTo("Yıldız");
        assertThat(response.employee().active()).isTrue();
        assertThat(response.employee().departmentId()).isEqualTo(department.getId());
        Employee saved = employeeRepository.findById(response.employee().id()).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
    }

    @Test
    void opensThisYearsAnnualAndExcuseBalancesInTheSameTransaction() {
        CreateEmployeeResponse response = mediator.send(threeYearsSeniority("deniz.yildiz@test.local"));

        assertThat(response.balances()).extracting(view -> view.leaveType().code())
                .containsExactly("ANNUAL", "EXCUSE");
        assertThat(response.balances()).extracting(BalanceView::year).containsOnly(today.getYear());
        assertThat(entitledDays(response.employee().id(), "ANNUAL")).isEqualByComparingTo("14");
        assertThat(entitledDays(response.employee().id(), "EXCUSE")).isEqualByComparingTo("5");
        // Hastalık ve ücretsiz izin bakiyeden düşmediği için açılmaz (BR-03).
        assertThat(countRows("leave_balances")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                "select distinct created_by from leave_mgmt.balance_transactions", Long.class))
                .containsExactly(hr.getId());
    }

    @Test
    void someoneHiredThisYearGetsAZeroAnnualBalanceWithoutATransaction() {
        CreateEmployeeResponse response = mediator.send(
                request("yeni@test.local", today.withDayOfYear(1), today.minusYears(25), null));

        assertThat(entitledDays(response.employee().id(), "ANNUAL")).isEqualByComparingTo("0");
        assertThat(entitledDays(response.employee().id(), "EXCUSE")).isEqualByComparingTo("5");
        assertThat(countRows("leave_balances")).isEqualTo(2);
        assertThat(countRows("balance_transactions")).isEqualTo(1);
    }

    @Test
    void setsTheManager() {
        Employee manager = employeeRepository.save(TestData.employee(department, Role.MANAGER));

        CreateEmployeeResponse response = mediator.send(
                request("ekip@test.local", today.minusYears(2), today.minusYears(28), manager.getId()));

        assertThat(response.employee().manager().id()).isEqualTo(manager.getId());
    }

    @Test
    void rejectsAnEmailThatIsAlreadyUsedIgnoringCase() {
        assertThatThrownBy(() -> mediator.send(threeYearsSeniority(hr.getEmail().toUpperCase())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VALUE);
        assertThat(countRows("employees")).isEqualTo(1);
    }

    @Test
    void aMissingDepartmentOrManagerIsNotFound() {
        CreateEmployeeRequest noDepartment = new CreateEmployeeRequest("Ad", "Soyad", "a@test.local", PASSWORD,
                today.minusYears(1), today.minusYears(30), Role.EMPLOYEE, 999_999L, null);
        assertThatThrownBy(() -> mediator.send(noDepartment))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);

        assertThatThrownBy(() -> mediator.send(
                request("b@test.local", today.minusYears(1), today.minusYears(30), 999_999L)))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
        assertThat(countRows("employees")).isEqualTo(1);
    }

    @Test
    void rejectsInvalidFields() {
        CreateEmployeeRequest invalid = new CreateEmployeeRequest("", "Soyad", "eposta-degil", "kisa",
                today, today.minusYears(30), null, null, 0L);

        assertThatThrownBy(() -> mediator.send(invalid))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("departmentId", "email", "firstName", "managerId", "password", "role"));
    }

    @Test
    void rejectsAFutureHireDateAndAnEmployeeYoungerThanFifteen() {
        assertThatThrownBy(() -> mediator.send(
                request("gelecek@test.local", today.plusDays(1), today.minusYears(14), null)))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("birthDate", "hireDate"));
        assertThat(countRows("employees")).isEqualTo(1);
    }

    @Test
    void theHireDateMustBeAfterTheBirthDate() {
        assertThatThrownBy(() -> mediator.send(
                request("ters@test.local", today.minusYears(40), today.minusYears(30), null)))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("hireDate"));
    }

    /**
     * T4'ün kanıtı: bakiye adımı hata verirse çalışan da oluşmaz. Çalışan satırı kimliği
     * veritabanında üretildiği için hata anında veritabanına gönderilmiştir; sıfır yeni satır
     * kalması rollback'in gerçekten olduğunu gösterir.
     */
    @Test
    void aFailureInTheBalanceStepRollsBackTheEmployee() {
        doThrow(new IllegalStateException("Test için bakiye adımında hata"))
                .when(entitlementPolicy).annualLeaveDays(any(), any(), anyInt());

        assertThatThrownBy(() -> mediator.send(threeYearsSeniority("geri.al@test.local")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test için bakiye adımında hata");

        assertThat(countRows("employees")).isEqualTo(1);
        assertThat(countRows("leave_balances")).isZero();
        assertThat(countRows("balance_transactions")).isZero();
        assertThat(employeeRepository.findByEmailIgnoreCase("geri.al@test.local")).isEmpty();
    }
}
