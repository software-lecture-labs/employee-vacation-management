package com.dogus.vacation.application.common.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dogus.vacation.application.common.view.LeaveRequestDetailView;
import com.dogus.vacation.application.common.view.LeaveRequestView;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link LeaveRequestMapper} için birim testleri. Veritabanı gerekmez. */
class LeaveRequestMapperTest {

    private Employee owner;
    private LeaveRequest leave;

    @BeforeEach
    void setUp() {
        owner = TestData.employee(TestData.department());
        LeaveType annual = new LeaveType("ANNUAL", "Yıllık izin", true, null);
        leave = TestData.leaveRequest(owner, annual,
                LocalDate.of(2027, 2, 5), LocalDate.of(2027, 2, 9), "3");
    }

    @Test
    void summaryViewCarriesListFields() {
        LeaveRequestView view = LeaveRequestMapper.toView(leave);

        assertThat(view.employee().fullName()).isEqualTo(owner.getFirstName() + " " + owner.getLastName());
        assertThat(view.leaveType().code()).isEqualTo("ANNUAL");
        assertThat(view.leaveType().name()).isEqualTo("Yıllık izin");
        assertThat(view.startDate()).isEqualTo(LocalDate.of(2027, 2, 5));
        assertThat(view.endDate()).isEqualTo(LocalDate.of(2027, 2, 9));
        assertThat(view.workingDays()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(view.status()).isEqualTo(LeaveRequestStatus.PENDING);
    }

    @Test
    void detailViewOfPendingRequestHasNoDecisionYet() {
        LeaveRequestDetailView view = LeaveRequestMapper.toDetailView(leave);

        assertThat(view.reason()).isEqualTo("Test talebi");
        assertThat(view.status()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(view.approver()).isNull();
        assertThat(view.decisionNote()).isNull();
        assertThat(view.cancelledBy()).isNull();
    }
}
