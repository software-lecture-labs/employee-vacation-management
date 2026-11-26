package com.dogus.vacation.application.usecase.history.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.HistoryEntryView;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.mediator.ValidationException;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveRequestHistory;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestHistoryRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestRepository;
import com.dogus.vacation.support.TestData;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Şirket izin geçmişi aramasını mediator üzerinden, gerçek PostgreSQL ile çalıştırır.
 *
 * <p>İki departmanda iki çalışanın iki talebi için dört geçmiş satırı kurulur:
 * <pre>
 * h1  1 Kas 09:00  A (dep. 1)  —        → PENDING
 * h2  2 Kas 10:00  A (dep. 1)  PENDING  → APPROVED   İK
 * h3  3 Kas 23:30  B (dep. 2)  —        → PENDING
 * h4  4 Kas 00:15  B (dep. 2)  PENDING  → REJECTED   İK, "Yoğun dönem"
 * </pre>
 * h3 ve h4 gece yarısının iki yanında: gün sınırı testleri bunlarla yapılır.
 */
class SearchLeaveHistoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Mediator mediator;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository requestRepository;

    @Autowired
    private LeaveRequestHistoryRepository historyRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Department departmentA;
    private Department departmentB;
    private Employee employeeA;
    private Employee employeeB;
    private Employee hr;
    private Long h1;
    private Long h2;
    private Long h3;
    private Long h4;

    @BeforeEach
    void createHistory() {
        departmentA = departmentRepository.save(TestData.department());
        departmentB = departmentRepository.save(TestData.department());
        employeeA = employeeRepository.save(TestData.employee(departmentA));
        employeeB = employeeRepository.save(TestData.employee(departmentB));
        hr = employeeRepository.save(TestData.employee(departmentA, Role.HR));
        LeaveRequest requestA = requestRepository.save(TestData.leaveRequest(employeeA, leaveType("ANNUAL"),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), "3"));
        LeaveRequest requestB = requestRepository.save(TestData.leaveRequest(employeeB, leaveType("EXCUSE"),
                LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 7), "1"));

        h1 = entry(requestA, null, LeaveRequestStatus.PENDING, null, null, "2026-11-01T09:00:00+03:00");
        h2 = entry(requestA, LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED, hr, null,
                "2026-11-02T10:00:00+03:00");
        h3 = entry(requestB, null, LeaveRequestStatus.PENDING, null, null, "2026-11-03T23:30:00+03:00");
        h4 = entry(requestB, LeaveRequestStatus.PENDING, LeaveRequestStatus.REJECTED, hr, "Yoğun dönem",
                "2026-11-04T00:15:00+03:00");
    }

    @Test
    void withoutFiltersReturnsEveryEventNewestFirst() {
        PageResponse<HistoryEntryView> result = search(null, null, null, null, null, null, null);

        assertThat(result.content()).extracting(HistoryEntryView::id).containsExactly(h4, h3, h2, h1);
        assertThat(result.totalElements()).isEqualTo(4);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void mapsEveryColumnOfAnEvent() {
        HistoryEntryView rejected = search(null, null, null, LeaveRequestStatus.REJECTED, null, null, null)
                .content().getFirst();

        assertThat(rejected.employee().id()).isEqualTo(employeeB.getId());
        assertThat(rejected.employee().fullName())
                .isEqualTo(employeeB.getFirstName() + " " + employeeB.getLastName());
        assertThat(rejected.departmentName()).isEqualTo(departmentB.getName());
        assertThat(rejected.leaveType().code()).isEqualTo("EXCUSE");
        assertThat(rejected.startDate()).isEqualTo(LocalDate.of(2026, 12, 7));
        assertThat(rejected.fromStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(rejected.toStatus()).isEqualTo(LeaveRequestStatus.REJECTED);
        assertThat(rejected.actor().id()).isEqualTo(hr.getId());
        assertThat(rejected.note()).isEqualTo("Yoğun dönem");
        assertThat(rejected.occurredAt()).isEqualTo(OffsetDateTime.parse("2026-11-04T00:15:00+03:00"));
    }

    @Test
    void dateFilterIncludesTheWholeOfBothDaysInIstanbulTime() {
        LocalDate nov3 = LocalDate.of(2026, 11, 3);

        assertThat(ids(search(nov3, nov3, null, null, null, null, null))).containsExactly(h3);
        assertThat(ids(search(LocalDate.of(2026, 11, 2), null, null, null, null, null, null)))
                .containsExactly(h4, h3, h2);
        assertThat(ids(search(null, LocalDate.of(2026, 11, 2), null, null, null, null, null)))
                .containsExactly(h2, h1);
    }

    @Test
    void filtersByDepartmentEmployeeAndStatus() {
        assertThat(ids(search(null, null, departmentA.getId(), null, null, null, null))).containsExactly(h2, h1);
        assertThat(ids(search(null, null, null, null, employeeB.getId(), null, null))).containsExactly(h4, h3);
        assertThat(ids(search(null, null, null, LeaveRequestStatus.PENDING, null, null, null)))
                .containsExactly(h3, h1);
    }

    @Test
    void combinesFilters() {
        assertThat(ids(search(null, null, departmentA.getId(), LeaveRequestStatus.APPROVED, null, null, null)))
                .containsExactly(h2);
        assertThat(ids(search(null, null, departmentA.getId(), null, employeeB.getId(), null, null))).isEmpty();
    }

    @Test
    void returnsTheRequestedPage() {
        PageResponse<HistoryEntryView> first = search(null, null, null, null, null, 0, 3);
        PageResponse<HistoryEntryView> second = search(null, null, null, null, null, 1, 3);

        assertThat(ids(first)).containsExactly(h4, h3, h2);
        assertThat(ids(second)).containsExactly(h1);
        assertThat(second.totalElements()).isEqualTo(4);
        assertThat(second.totalPages()).isEqualTo(2);
    }

    @Test
    void rejectsAReversedRangeAndAnInvalidPageSize() {
        assertThatThrownBy(() -> search(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1),
                null, null, null, null, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> search(null, null, null, null, null, 0, 101))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void loadsAPageWithoutAQueryPerRow() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            PageResponse<HistoryEntryView> result = search(null, null, null, null, null, 0, 3);

            assertThat(result.content()).hasSize(3);
            // Sayfanın SELECT'i ve toplam için COUNT; ilişkiler için satır başına sorgu yok.
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    // ---- yardımcılar ----

    private Long entry(LeaveRequest request, LeaveRequestStatus from, LeaveRequestStatus to, Employee actor,
                       String note, String occurredAt) {
        return historyRepository.save(new LeaveRequestHistory(request, from, to, actor, note,
                OffsetDateTime.parse(occurredAt))).getId();
    }

    private PageResponse<HistoryEntryView> search(LocalDate from, LocalDate to, Long departmentId,
                                                  LeaveRequestStatus status, Long employeeId,
                                                  Integer page, Integer size) {
        return mediator.send(new SearchLeaveHistoryRequest(from, to, departmentId, status, employeeId, page, size))
                .history();
    }

    private static List<Long> ids(PageResponse<HistoryEntryView> result) {
        return result.content().stream().map(HistoryEntryView::id).toList();
    }
}
