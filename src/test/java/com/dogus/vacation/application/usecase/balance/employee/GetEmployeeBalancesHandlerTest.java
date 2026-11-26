package com.dogus.vacation.application.usecase.balance.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.ForbiddenActionException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link GetEmployeeBalancesHandler}'ın kayda bağlı yetki kuralının HTTP üzerinden denenemeyen
 * dalları ve istenen yılın kullanılması.
 *
 * <p>Çalışan rolü Controller'daki {@code @PreAuthorize} yüzünden buraya hiç ulaşmaz; Handler yine
 * de onu reddetmelidir, çünkü ileride aynı Handler başka bir giriş katmanından (SOAP, gRPC)
 * çağrılabilir ve o katmanda anotasyon olmayabilir.
 */
class GetEmployeeBalancesHandlerTest {

    private EmployeeRepository employeeRepository;
    private LeaveBalanceRepository balanceRepository;
    private CurrentUserProvider currentUser;
    private GetEmployeeBalancesHandler handler;

    private Employee teamManager;
    private Employee teamMember;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        balanceRepository = mock(LeaveBalanceRepository.class);
        currentUser = mock(CurrentUserProvider.class);
        handler = new GetEmployeeBalancesHandler(employeeRepository, balanceRepository, currentUser,
                Clock.fixed(Instant.parse("2026-11-26T09:00:00Z"), ZoneId.of("Europe/Istanbul")));

        Department department = TestData.department();
        teamManager = withId(TestData.employee(department, Role.MANAGER), 5L);
        teamMember = withId(TestData.employee(department, Role.EMPLOYEE), 12L);
        teamMember.setManager(teamManager);
        when(employeeRepository.findById(12L)).thenReturn(Optional.of(teamMember));
    }

    @Test
    void anEmployeeCannotSeeSomeoneElseEvenIfTheControllerCheckIsBypassed() {
        when(currentUser.currentEmployee()).thenReturn(withId(TestData.employee(TestData.department()), 31L));

        assertThatThrownBy(() -> handler.handle(new GetEmployeeBalancesRequest(12L, null)))
                .isInstanceOf(ForbiddenActionException.class)
                .extracting(e -> ((ForbiddenActionException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);
        verify(balanceRepository, never()).findWithLeaveTypeByEmployeeIdAndYear(anyLong(), anyInt());
    }

    @Test
    void aManagerCannotSeeAnotherManagersTeamMember() {
        when(currentUser.currentEmployee()).thenReturn(withId(TestData.employee(TestData.department(), Role.MANAGER), 6L));

        assertThatThrownBy(() -> handler.handle(new GetEmployeeBalancesRequest(12L, null)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void usesTheRequestedYearInsteadOfThisYear() {
        when(currentUser.currentEmployee()).thenReturn(teamManager);
        when(balanceRepository.findWithLeaveTypeByEmployeeIdAndYear(12L, 2025)).thenReturn(List.of());

        GetEmployeeBalancesResponse response = handler.handle(new GetEmployeeBalancesRequest(12L, 2025));

        assertThat(response.year()).isEqualTo(2025);
        assertThat(response.employee().id()).isEqualTo(12L);
        verify(balanceRepository).findWithLeaveTypeByEmployeeIdAndYear(12L, 2025);
    }

    private static Employee withId(Employee employee, Long id) {
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }
}
