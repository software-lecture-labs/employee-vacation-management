package com.dogus.vacation.application.usecase.cancellation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.common.CurrentUserProvider;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.application.usecase.cancellation.approve.ApproveLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.approve.ApproveLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.cancel.CancelLeaveRequest;
import com.dogus.vacation.application.usecase.cancellation.cancel.CancelLeaveResponse;
import com.dogus.vacation.application.usecase.cancellation.reject.RejectLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.request.RequestLeaveCancellationRequest;
import com.dogus.vacation.application.usecase.cancellation.request.RequestLeaveCancellationResponse;
import com.dogus.vacation.application.usecase.cancellation.withdraw.WithdrawLeaveCancellationRequest;
import com.dogus.vacation.domain.exception.BusinessException;
import com.dogus.vacation.domain.exception.ErrorCode;
import com.dogus.vacation.domain.exception.NotFoundException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.BalanceTransaction;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.BalanceTransactionRepository;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * İptal akışını (FR-23, FR-25, FR-28, FR-29; BR-08, BR-09, BR-15, BR-17; T2) mediator üzerinden,
 * gerçek PostgreSQL ile çalıştırır.
 *
 * <p>Kurulum: yönetici Ahmet ve ona bağlı Ayşe; yöneticisi olmayan Deniz; iki İK. Ayşe'nin
 * 14 günlük yıllık izninden 3 günlük, 10 gün sonra başlayan bir izni onaylanmış (bakiyeden
 * düşülmüş) durumda. İşlemi yapan kişi {@link CurrentUserProvider} sahtesiyle verilir; sonuçlar
 * transaction kapandıktan sonra SQL ile okunur.
 */
class LeaveCancellationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Mediator mediator;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository balanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private Clock clock;

    @MockitoBean
    private CurrentUserProvider currentUser;

    /**
     * T2 rollback testleri iade hareketinin yazıldığı son adımı bozar. Servisin kendisi değil
     * repository'si kullanılır: servis yalnızca açık bir transaction içinde çağrılabildiği
     * ({@code MANDATORY}) için testten doğrudan ayarlanamaz.
     */
    @MockitoSpyBean
    private BalanceTransactionRepository transactionRepository;

    private LocalDate today;
    private LeaveType annual;
    private Employee hr1;
    private Employee hr2;
    private Employee manager;
    private Employee ayse;
    private Employee deniz;
    private LeaveRequest ayseLeave;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(clock);
        annual = leaveTypeRepository.findByCode("ANNUAL").orElseThrow();
        Department department = departmentRepository.save(TestData.department());
        hr1 = employeeRepository.save(TestData.employee(department, Role.HR));
        hr2 = employeeRepository.save(TestData.employee(department, Role.HR));
        manager = employeeRepository.save(TestData.employee(department, Role.MANAGER));
        Employee newAyse = TestData.employee(department, Role.EMPLOYEE);
        newAyse.setManager(manager);
        ayse = employeeRepository.save(newAyse);
        deniz = employeeRepository.save(TestData.employee(department, Role.EMPLOYEE));
        ayseLeave = approvedLeave(ayse, manager, today.plusDays(10));
    }

    /** Onaylanmış ve günleri bakiyeden düşülmüş 3 günlük bir izin (onay akışının sonucu). */
    private LeaveRequest approvedLeave(Employee owner, Employee approver, LocalDate start) {
        LeaveBalance balance = TestData.balance(owner, annual, start.getYear(), "14");
        balance.use(new BigDecimal("3"));
        balanceRepository.save(balance);
        LeaveRequest leave = TestData.leaveRequest(owner, annual, start, start.plusDays(2), "3");
        leave.approve(approver, today, OffsetDateTime.now(clock));
        return leaveRequestRepository.save(leave);
    }

    private void actingAs(Employee actor) {
        when(currentUser.currentEmployee()).thenReturn(actor);
    }

    private String statusOf(LeaveRequest leave) {
        return jdbcTemplate.queryForObject("select status from leave_mgmt.leave_requests where id = ?",
                String.class, leave.getId());
    }

    private BigDecimal usedDays(Employee employee) {
        return jdbcTemplate.queryForObject(
                "select used_days from leave_mgmt.leave_balances where employee_id = ? and leave_type_id = ?",
                BigDecimal.class, employee.getId(), annual.getId());
    }

    private List<Map<String, Object>> history(LeaveRequest leave) {
        return jdbcTemplate.queryForList("""
                select from_status, to_status, actor_id, note from leave_mgmt.leave_request_history
                where leave_request_id = ? order by id""", leave.getId());
    }

    private List<String> refundsOf(LeaveRequest leave) {
        return jdbcTemplate.queryForList("""
                select type from leave_mgmt.balance_transactions where leave_request_id = ?""",
                String.class, leave.getId());
    }

    /** İade hareketi kaydedilirken hata: o ana kadar durum, geçmiş ve bakiye değişmiş olur. */
    private void failWhenTheRefundIsRecorded() {
        doThrow(new IllegalStateException("Test için iade adımında hata"))
                .when(transactionRepository).save(any(BalanceTransaction.class));
    }

    private static ErrorCode codeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    // ---- FR-23: şirket adına iptal (T2) ----

    @Test
    void theManagerCancelsAnApprovedLeaveAndTheDaysAreRefunded() {
        actingAs(manager);

        CancelLeaveResponse response = mediator.send(new CancelLeaveRequest(ayseLeave.getId(), " Proje teslimi "));

        assertThat(response.leaveRequest().status()).isEqualTo(LeaveRequestStatus.CANCELLED);
        assertThat(response.leaveRequest().cancelReason()).isEqualTo("Proje teslimi");
        assertThat(response.leaveRequest().cancelledBy().id()).isEqualTo(manager.getId());
        assertThat(statusOf(ayseLeave)).isEqualTo("CANCELLED");
        assertThat(usedDays(ayse)).isEqualByComparingTo("0");
        assertThat(refundsOf(ayseLeave)).containsExactly("REFUND");
        assertThat(history(ayseLeave)).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("from_status", "APPROVED").containsEntry("to_status", "CANCELLED")
                    .containsEntry("note", "Proje teslimi");
            assertThat(((Number) row.get("actor_id")).longValue()).isEqualTo(manager.getId());
        });
    }

    @Test
    void anHrCannotCancelTheLeaveOfSomeoneWhoHasAManager() {
        actingAs(hr1);

        assertThatThrownBy(() -> mediator.send(new CancelLeaveRequest(ayseLeave.getId(), "Neden")))
                .isInstanceOf(BusinessException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);
        assertThat(statusOf(ayseLeave)).isEqualTo("APPROVED");
    }

    @Test
    void anHrCancelsTheLeaveOfSomeoneWithoutAManager() {
        LeaveRequest denizLeave = approvedLeave(deniz, hr1, today.plusDays(10));
        actingAs(hr2);

        mediator.send(new CancelLeaveRequest(denizLeave.getId(), "Yoğun dönem"));

        assertThat(statusOf(denizLeave)).isEqualTo("CANCELLED");
        assertThat(usedDays(deniz)).isEqualByComparingTo("0");
    }

    @Test
    void anotherHrCancelsAnHrLeaveButNotTheirOwn() {
        LeaveRequest hrLeave = approvedLeave(hr1, hr2, today.plusDays(10));

        actingAs(hr1);
        assertThatThrownBy(() -> mediator.send(new CancelLeaveRequest(hrLeave.getId(), "Kendi iznim")))
                .isInstanceOf(BusinessException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);

        actingAs(hr2);
        mediator.send(new CancelLeaveRequest(hrLeave.getId(), "Denetim haftası"));
        assertThat(statusOf(hrLeave)).isEqualTo("CANCELLED");
    }

    @Test
    void aStartedLeaveCannotBeCancelled() {
        LeaveRequest started = approvedLeave(deniz, hr1, today);
        actingAs(hr2);

        assertThatThrownBy(() -> mediator.send(new CancelLeaveRequest(started.getId(), "Geç kaldık")))
                .isInstanceOf(BusinessException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        assertThat(statusOf(started)).isEqualTo("APPROVED");
        assertThat(usedDays(deniz)).isEqualByComparingTo("3");
    }

    @Test
    void aPendingLeaveCannotBeCancelled() {
        LeaveRequest pending = leaveRequestRepository.save(
                TestData.leaveRequest(ayse, annual, today.plusDays(20), today.plusDays(21), "2"));
        actingAs(manager);

        assertThatThrownBy(() -> mediator.send(new CancelLeaveRequest(pending.getId(), "Neden")))
                .isInstanceOf(BusinessException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void theReasonIsRequired() {
        actingAs(manager);

        assertThatThrownBy(() -> mediator.send(new CancelLeaveRequest(ayseLeave.getId(), "  ")))
                .isInstanceOf(ValidationException.class)
                .extracting(e -> ((ValidationException) e).getErrors().stream().map(ValidationError::field).toList())
                .isEqualTo(List.of("reason"));
    }

    @Test
    void aMissingLeaveRequestIsNotFound() {
        actingAs(manager);

        assertThatThrownBy(() -> mediator.send(new CancelLeaveRequest(999_999L, "Neden")))
                .isInstanceOf(NotFoundException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.LEAVE_REQUEST_NOT_FOUND);
    }

    /**
     * T2'nin kanıtı: gün iadesi hata verirse iptal de geri alınır. Hata anında durum değişmiş,
     * geçmiş satırı veritabanına yazılmış ve bakiye artırılmıştır; sonuçta izin APPROVED kalır,
     * geçmiş satırı ve iade hareketi yoktur, kullanılan gün değişmez.
     */
    @Test
    void aFailureInTheRefundRollsBackTheCancellation() {
        failWhenTheRefundIsRecorded();
        actingAs(manager);

        assertThatThrownBy(() -> mediator.send(new CancelLeaveRequest(ayseLeave.getId(), "Proje teslimi")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test için iade adımında hata");

        assertThat(statusOf(ayseLeave)).isEqualTo("APPROVED");
        assertThat(history(ayseLeave)).isEmpty();
        assertThat(refundsOf(ayseLeave)).isEmpty();
        assertThat(usedDays(ayse)).isEqualByComparingTo("3");
    }

    // ---- FR-25: iptal talebi açma ----

    @Test
    void theOwnerRequestsACancellationAndNoDaysAreRefundedYet() {
        actingAs(ayse);

        RequestLeaveCancellationResponse response = mediator.send(
                new RequestLeaveCancellationRequest(ayseLeave.getId(), "Planlar değişti"));

        assertThat(response.leaveRequest().status()).isEqualTo(LeaveRequestStatus.CANCEL_REQUESTED);
        assertThat(response.leaveRequest().cancelRequestReason()).isEqualTo("Planlar değişti");
        assertThat(usedDays(ayse)).isEqualByComparingTo("3");
        assertThat(history(ayseLeave)).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("to_status", "CANCEL_REQUESTED"));
    }

    @Test
    void onlyTheOwnerCanRequestACancellation() {
        actingAs(manager);

        assertThatThrownBy(() -> mediator.send(new RequestLeaveCancellationRequest(ayseLeave.getId(), "Neden")))
                .isInstanceOf(BusinessException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);
    }

    @Test
    void noCancellationRequestForAStartedLeave() {
        LeaveRequest started = approvedLeave(deniz, hr1, today);
        actingAs(deniz);

        assertThatThrownBy(() -> mediator.send(new RequestLeaveCancellationRequest(started.getId(), "Neden")))
                .isInstanceOf(BusinessException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    // ---- FR-28: iptal talebini kabul ve red (T2) ----

    private void ayseRequestsCancellation() {
        actingAs(ayse);
        mediator.send(new RequestLeaveCancellationRequest(ayseLeave.getId(), "Planlar değişti"));
    }

    @Test
    void theManagerApprovesTheCancellationRequestAndTheDaysAreRefunded() {
        ayseRequestsCancellation();
        actingAs(manager);

        ApproveLeaveCancellationResponse response =
                mediator.send(new ApproveLeaveCancellationRequest(ayseLeave.getId()));

        assertThat(response.leaveRequest().status()).isEqualTo(LeaveRequestStatus.CANCELLED);
        assertThat(response.leaveRequest().cancelReason()).isEqualTo("Planlar değişti");
        assertThat(usedDays(ayse)).isEqualByComparingTo("0");
        assertThat(refundsOf(ayseLeave)).containsExactly("REFUND");
        assertThat(history(ayseLeave)).extracting(row -> row.get("to_status"))
                .containsExactly("CANCEL_REQUESTED", "CANCELLED");
    }

    @Test
    void theOwnerCannotApproveTheirOwnCancellationRequest() {
        ayseRequestsCancellation();

        assertThatThrownBy(() -> mediator.send(new ApproveLeaveCancellationRequest(ayseLeave.getId())))
                .isInstanceOf(BusinessException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.SELF_APPROVAL_NOT_ALLOWED);
    }

    @Test
    void aFailureInTheRefundKeepsTheCancellationRequestOpen() {
        ayseRequestsCancellation();
        failWhenTheRefundIsRecorded();
        actingAs(manager);

        assertThatThrownBy(() -> mediator.send(new ApproveLeaveCancellationRequest(ayseLeave.getId())))
                .isInstanceOf(IllegalStateException.class);

        assertThat(statusOf(ayseLeave)).isEqualTo("CANCEL_REQUESTED");
        assertThat(history(ayseLeave)).hasSize(1);
        assertThat(usedDays(ayse)).isEqualByComparingTo("3");
    }

    @Test
    void theManagerRejectsTheCancellationRequestAndTheLeaveStaysApproved() {
        ayseRequestsCancellation();
        actingAs(manager);

        mediator.send(new RejectLeaveCancellationRequest(ayseLeave.getId(), "Ekip eksik kalır"));

        assertThat(statusOf(ayseLeave)).isEqualTo("APPROVED");
        assertThat(usedDays(ayse)).isEqualByComparingTo("3");
        assertThat(refundsOf(ayseLeave)).isEmpty();
        assertThat(history(ayseLeave)).last().satisfies(row -> assertThat(row)
                .containsEntry("from_status", "CANCEL_REQUESTED").containsEntry("to_status", "APPROVED")
                .containsEntry("note", "Ekip eksik kalır"));
    }

    // ---- FR-29: iptal talebinden vazgeçme ----

    @Test
    void theOwnerWithdrawsTheCancellationRequest() {
        ayseRequestsCancellation();

        mediator.send(new WithdrawLeaveCancellationRequest(ayseLeave.getId()));

        assertThat(statusOf(ayseLeave)).isEqualTo("APPROVED");
        assertThat(usedDays(ayse)).isEqualByComparingTo("3");
    }

    @Test
    void onlyTheOwnerCanWithdrawTheCancellationRequest() {
        ayseRequestsCancellation();
        actingAs(manager);

        assertThatThrownBy(() -> mediator.send(new WithdrawLeaveCancellationRequest(ayseLeave.getId())))
                .isInstanceOf(BusinessException.class)
                .extracting(LeaveCancellationIntegrationTest::codeOf)
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);
        assertThat(statusOf(ayseLeave)).isEqualTo("CANCEL_REQUESTED");
    }
}
