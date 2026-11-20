package com.dogus.vacation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.dogus.vacation.domain.exception.InvalidStateTransitionException;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link LeaveRequest} durum geçişleri için birim testleri. Veritabanı gerekmez.
 *
 * <p>İzin 2027-02-08 Pazartesi – 2027-02-10 Çarşamba; "bugün" 2027-02-01.
 */
class LeaveRequestTest {

    private static final LocalDate START = LocalDate.of(2027, 2, 8);
    private static final LocalDate END = LocalDate.of(2027, 2, 10);
    private static final LocalDate TODAY = LocalDate.of(2027, 2, 1);
    private static final OffsetDateTime NOW = OffsetDateTime.of(2027, 2, 1, 10, 0, 0, 0, ZoneOffset.ofHours(3));

    private Employee owner;
    private Employee manager;
    private LeaveRequest leave;

    @BeforeEach
    void setUp() {
        Department department = TestData.department();
        owner = TestData.employee(department);
        manager = TestData.employee(department, Role.MANAGER);
        leave = TestData.leaveRequest(owner, null, START, END, "3");
    }

    @Test
    void newRequestIsPending() {
        assertThat(leave.getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(leave.isOwnedBy(owner)).isTrue();
        assertThat(leave.isOwnedBy(manager)).isFalse();
    }

    @Test
    void approveRecordsDecision() {
        leave.approve(manager, TODAY, NOW);

        assertThat(leave.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(leave.getApprover()).isSameAs(manager);
        assertThat(leave.getDecidedAt()).isEqualTo(NOW);
        assertThat(leave.getDecisionNote()).isNull();
    }

    @Test
    void pendingRequestCanStillBeApprovedOnItsLastDay() {
        leave.approve(manager, END, NOW);

        assertThat(leave.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
    }

    @Test
    void requestWhoseEndDatePassedCannotBeApprovedButCanBeRejected() {
        LocalDate afterEnd = END.plusDays(1);

        assertThatThrownBy(() -> leave.approve(manager, afterEnd, NOW))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("sadece reddedilebilir");

        leave.reject(manager, "Geç kalındı", NOW);
        assertThat(leave.getStatus()).isEqualTo(LeaveRequestStatus.REJECTED);
    }

    @Test
    void rejectRecordsReason() {
        leave.reject(manager, "  Yoğun dönem  ", NOW);

        assertThat(leave.getStatus()).isEqualTo(LeaveRequestStatus.REJECTED);
        assertThat(leave.getDecisionNote()).isEqualTo("Yoğun dönem");
        assertThat(leave.getApprover()).isSameAs(manager);
    }

    @Test
    void rejectRequiresReason() {
        assertThatThrownBy(() -> leave.reject(manager, " ", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThat(leave.getStatus()).isEqualTo(LeaveRequestStatus.PENDING);
    }

    @Test
    void rejectedRequestCannotBeApproved() {
        leave.reject(manager, "Yoğun dönem", NOW);

        assertThatThrownBy(() -> leave.approve(manager, TODAY, NOW))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Talep 'Reddedildi' durumundayken onaylanamaz.");
    }

    @Test
    void onlyPendingRequestCanBeWithdrawn() {
        leave.withdraw();
        assertThat(leave.getStatus()).isEqualTo(LeaveRequestStatus.WITHDRAWN);

        LeaveRequest approved = approvedLeave();
        assertThatThrownBy(approved::withdraw).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancellationRequestBeforeStart() {
        LeaveRequest approved = approvedLeave();

        approved.requestCancellation("Planım değişti", TODAY, NOW);

        assertThat(approved.getStatus()).isEqualTo(LeaveRequestStatus.CANCEL_REQUESTED);
        assertThat(approved.getCancelRequestReason()).isEqualTo("Planım değişti");
        assertThat(approved.getCancelRequestedAt()).isEqualTo(NOW);
    }

    @Test
    void cancellationRequestNotAllowedOnStartDayOrForPendingRequest() {
        LeaveRequest approved = approvedLeave();

        assertThatThrownBy(() -> approved.requestCancellation("Planım değişti", START, NOW))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> leave.requestCancellation("Planım değişti", TODAY, NOW))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void withdrawingOrRejectingCancellationRequestKeepsLeaveApproved() {
        LeaveRequest first = cancelRequestedLeave();
        first.withdrawCancellationRequest();
        assertThat(first.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(first.getCancelRequestReason()).isNull();

        LeaveRequest second = cancelRequestedLeave();
        second.rejectCancellationRequest();
        assertThat(second.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(second.getCancelRequestedAt()).isNull();
    }

    @Test
    void approvingCancellationRequestCancelsLeaveWithRequestReason() {
        LeaveRequest requested = cancelRequestedLeave();

        requested.approveCancellationRequest(manager, TODAY, NOW);

        assertThat(requested.getStatus()).isEqualTo(LeaveRequestStatus.CANCELLED);
        assertThat(requested.getCancelledBy()).isSameAs(manager);
        assertThat(requested.getCancelReason()).isEqualTo("Planım değişti");
        assertThat(requested.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void cancellationRequestCannotBeApprovedOnceLeaveStarted() {
        LeaveRequest requested = cancelRequestedLeave();

        assertThatThrownBy(() -> requested.approveCancellationRequest(manager, START, NOW))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void companyCancellationWorksForApprovedAndCancelRequestedLeaves() {
        LeaveRequest approved = approvedLeave();
        approved.cancel(manager, "Proje teslimi", TODAY, NOW);
        assertThat(approved.getStatus()).isEqualTo(LeaveRequestStatus.CANCELLED);
        assertThat(approved.getCancelReason()).isEqualTo("Proje teslimi");

        LeaveRequest requested = cancelRequestedLeave();
        requested.cancel(manager, "Proje teslimi", TODAY, NOW);
        assertThat(requested.getStatus()).isEqualTo(LeaveRequestStatus.CANCELLED);
    }

    @Test
    void startedLeaveCannotBeCancelled() {
        LeaveRequest approved = approvedLeave();

        assertThatThrownBy(() -> approved.cancel(manager, "Proje teslimi", START, NOW))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Başlamış bir izin iptal edilemez.");
    }

    @Test
    void cancellationRequestExpiresOnStartDayOnly() {
        LeaveRequest requested = cancelRequestedLeave();
        LocalDate dayBeforeStart = START.minusDays(1);

        assertThatThrownBy(() -> requested.expireCancellationRequest(dayBeforeStart))
                .isInstanceOf(InvalidStateTransitionException.class);

        requested.expireCancellationRequest(START);
        assertThat(requested.getStatus()).isEqualTo(LeaveRequestStatus.CANCEL_REQUEST_EXPIRED);
    }

    @Test
    void finalStatusAllowsNoTransition() {
        leave.withdraw();

        assertThatThrownBy(() -> leave.approve(manager, TODAY, NOW)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> leave.reject(manager, "Gerekçe", NOW)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(leave::withdraw).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> leave.cancel(manager, "Gerekçe", TODAY, NOW))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(LeaveRequestStatus.class)
    void everyStatusHasTurkishLabel(LeaveRequestStatus status) {
        assertThat(status.label()).isNotBlank();
    }

    @Test
    void onlyEndStatesAreFinal() {
        assertThat(LeaveRequestStatus.PENDING.isFinal()).isFalse();
        assertThat(LeaveRequestStatus.APPROVED.isFinal()).isFalse();
        assertThat(LeaveRequestStatus.CANCEL_REQUESTED.isFinal()).isFalse();
        assertThat(LeaveRequestStatus.REJECTED.isFinal()).isTrue();
        assertThat(LeaveRequestStatus.WITHDRAWN.isFinal()).isTrue();
        assertThat(LeaveRequestStatus.CANCELLED.isFinal()).isTrue();
        assertThat(LeaveRequestStatus.CANCEL_REQUEST_EXPIRED.isFinal()).isTrue();
    }

    private LeaveRequest approvedLeave() {
        LeaveRequest approved = TestData.leaveRequest(owner, null, START, END, "3");
        approved.approve(manager, TODAY, NOW);
        return approved;
    }

    private LeaveRequest cancelRequestedLeave() {
        LeaveRequest requested = approvedLeave();
        requested.requestCancellation("Planım değişti", TODAY, NOW);
        return requested;
    }
}
