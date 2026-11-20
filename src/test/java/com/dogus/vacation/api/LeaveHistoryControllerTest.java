package com.dogus.vacation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.dogus.vacation.api.advice.GlobalExceptionHandler;
import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.EmployeeRef;
import com.dogus.vacation.application.common.view.HistoryEntryView;
import com.dogus.vacation.application.common.view.LeaveTypeRef;
import com.dogus.vacation.application.mediator.Mediator;
import com.dogus.vacation.application.usecase.history.search.SearchLeaveHistoryRequest;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@link LeaveHistoryController}'ın sorgu parametrelerini isteğe doğru çevirdiğini ve sayfalı
 * cevabın JSON biçimini doğrular. Mediator sahtedir; Spring context açılmaz.
 */
class LeaveHistoryControllerTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Europe/Istanbul"));

    private static final HistoryEntryView APPROVED = new HistoryEntryView(
            7L, OffsetDateTime.parse("2026-11-02T10:00:00+03:00"), 42L,
            new EmployeeRef(3L, "Ayşe Yılmaz"), "Yazılım", new LeaveTypeRef(1L, "ANNUAL", "Yıllık izin"),
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3),
            LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED, new EmployeeRef(9L, "Mehmet Kaya"), null);

    private Mediator mediator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mediator = mock(Mediator.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LeaveHistoryController(mediator))
                .setControllerAdvice(new GlobalExceptionHandler(CLOCK))
                .build();
    }

    @Test
    void passesEveryFilterToTheUseCase() throws Exception {
        when(mediator.send(any(SearchLeaveHistoryRequest.class)))
                .thenReturn(new PageResponse<>(List.of(APPROVED), 1, 5, 6, 2));

        mockMvc.perform(get("/api/leave-requests/history")
                        .param("from", "2026-11-01").param("to", "2026-11-30")
                        .param("departmentId", "2").param("status", "APPROVED").param("employeeId", "3")
                        .param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].leaveRequestId").value(42))
                .andExpect(jsonPath("$.content[0].employee.fullName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.content[0].toStatus").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].actor.fullName").value("Mehmet Kaya"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
        verify(mediator).send(new SearchLeaveHistoryRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                2L, LeaveRequestStatus.APPROVED, 3L, 1, 5));
    }

    @Test
    void usesDefaultsWhenNoParameterIsGiven() throws Exception {
        when(mediator.send(any(SearchLeaveHistoryRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/leave-requests/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        verify(mediator).send(new SearchLeaveHistoryRequest(null, null, null, null, null, 0, 20));
    }

    @Test
    void rejectsAnUnknownStatusWith400() throws Exception {
        mockMvc.perform(get("/api/leave-requests/history").param("status", "APROVED"))
                .andExpect(status().isBadRequest());
        verify(mediator, never()).send(any());
    }

    @Test
    void rejectsADateInAnotherFormatWith400() throws Exception {
        mockMvc.perform(get("/api/leave-requests/history").param("from", "01.11.2026"))
                .andExpect(status().isBadRequest());
        verify(mediator, never()).send(any());
    }
}
