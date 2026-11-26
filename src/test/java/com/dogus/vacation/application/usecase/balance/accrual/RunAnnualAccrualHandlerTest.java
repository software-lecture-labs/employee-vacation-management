package com.dogus.vacation.application.usecase.balance.accrual;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.model.AnnualAccrualRun;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.domain.service.LeaveBalanceService;
import com.dogus.vacation.infrastructure.persistence.AnnualAccrualRunRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * {@link RunAnnualAccrualHandler}'ın veritabanıyla tekrarlanması zor bir durumu: iki İK aynı anda
 * çalıştırdığında ikisi de ilk kontrolü geçer, ikinci kayıt {@code uq_annual_accrual_runs_year}
 * kısıtına takılır. Bu hata 500 değil, ilk kontroldeki gibi 409 olmalı.
 */
class RunAnnualAccrualHandlerTest {

    @Test
    void aConcurrentRunForTheSameYearIsReportedAsAlreadyRun() {
        AnnualAccrualRunRepository runRepository = mock(AnnualAccrualRunRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        LeaveTypeRepository leaveTypeRepository = mock(LeaveTypeRepository.class);
        CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
        when(runRepository.existsByYear(2027)).thenReturn(false);
        when(currentUser.currentEmployee()).thenReturn(TestData.employee(TestData.department(), Role.HR));
        when(leaveTypeRepository.findAllByActiveTrueAndDeductsFromBalanceTrueOrderByIdAsc()).thenReturn(List.of());
        when(employeeRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(List.of());
        when(runRepository.saveAndFlush(any(AnnualAccrualRun.class)))
                .thenThrow(new DataIntegrityViolationException("uq_annual_accrual_runs_year"));
        RunAnnualAccrualHandler handler = new RunAnnualAccrualHandler(runRepository, employeeRepository,
                leaveTypeRepository, mock(LeaveBalanceService.class), currentUser,
                Clock.fixed(Instant.parse("2026-12-15T07:30:00Z"), ZoneId.of("Europe/Istanbul")));

        assertThatThrownBy(() -> handler.handle(new RunAnnualAccrualRequest(2027)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("2027 yılının hak edişi zaten çalıştırıldı.")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCRUAL_ALREADY_RUN);
    }
}
