package com.dogus.vacation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveRequestHistory;
import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestHistoryRepository;
import com.dogus.vacation.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link LeaveHistoryService}'in kurallarını veritabanı olmadan, saati sabitlenmiş bir
 * {@link Clock} ve sahte (Mockito) repository ile doğrular. Kaydın gerçekten yazıldığını
 * ve geri alındığını {@code LeaveHistoryServiceIntegrationTest} gösterir.
 */
class LeaveHistoryServiceTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final Instant NOW = Instant.parse("2026-11-20T10:15:00Z");

    private LeaveRequestHistoryRepository repository;
    private LeaveHistoryService service;

    private Employee hr;
    private LeaveRequest request;

    @BeforeEach
    void setUp() {
        repository = mock(LeaveRequestHistoryRepository.class);
        when(repository.saveAndFlush(any(LeaveRequestHistory.class))).thenAnswer(call -> call.getArgument(0));
        service = new LeaveHistoryService(repository, Clock.fixed(NOW, ISTANBUL));

        Employee employee = TestData.employee(TestData.department());
        hr = TestData.employee(TestData.department(), Role.HR);
        request = TestData.leaveRequest(employee, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), "3");
    }

    @Test
    void recordsTheTransitionToTheCurrentStatus() {
        request.withdraw();

        LeaveRequestHistory entry = service.record(request, LeaveRequestStatus.PENDING, hr, "Planım değişti");

        assertThat(entry.getLeaveRequest()).isSameAs(request);
        assertThat(entry.getFromStatus()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(entry.getToStatus()).isEqualTo(LeaveRequestStatus.WITHDRAWN);
        assertThat(entry.getActor()).isSameAs(hr);
        assertThat(entry.getNote()).isEqualTo("Planım değişti");
        assertThat(entry.getOccurredAt()).isEqualTo(OffsetDateTime.ofInstant(NOW, ISTANBUL));
        verify(repository).saveAndFlush(entry);
    }

    @Test
    void recordsANewRequestWithoutAPreviousStatus() {
        LeaveRequestHistory entry = service.record(request, null, hr, null);

        assertThat(entry.getFromStatus()).isNull();
        assertThat(entry.getToStatus()).isEqualTo(LeaveRequestStatus.PENDING);
    }

    @Test
    void recordsASystemTransitionWithoutAnActor() {
        request.withdraw();

        LeaveRequestHistory entry = service.record(request, LeaveRequestStatus.PENDING, null, null);

        assertThat(entry.getActor()).isNull();
    }

    @Test
    void stripsTheNoteAndStoresABlankNoteAsNull() {
        request.withdraw();

        assertThat(service.record(request, LeaveRequestStatus.PENDING, hr, "  Gerekçe  ").getNote())
                .isEqualTo("Gerekçe");
        assertThat(service.record(request, LeaveRequestStatus.PENDING, hr, "   ").getNote())
                .isNull();
    }

    @Test
    void rejectsANoteLongerThanTheColumn() {
        request.withdraw();
        String longNote = "a".repeat(LeaveHistoryService.MAX_NOTE_LENGTH + 1);

        assertThatThrownBy(() -> service.record(request, LeaveRequestStatus.PENDING, hr, longNote))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsWhenTheStatusHasNotChanged() {
        assertThatThrownBy(() -> service.record(request, LeaveRequestStatus.PENDING, hr, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Durum değişmemiş");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAMissingPreviousStatusUnlessTheRequestIsNew() {
        request.withdraw();

        assertThatThrownBy(() -> service.record(request, null, hr, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDING");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAMissingRequest() {
        assertThatThrownBy(() -> service.record(null, LeaveRequestStatus.PENDING, hr, null))
                .isInstanceOf(NullPointerException.class);
    }
}
