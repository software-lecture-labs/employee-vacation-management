package com.dogus.vacation.domain.policy;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * Pazartesi–Cuma günlerini sayan iş günü hesabı (BR-01).
 *
 * <p>Kanunda Cumartesi de iş günü sayılır; proje 5 günlük çalışma haftası varsayar.
 * Resmi tatiller bu sınıfın kapsamında değildir (bonus, FR-38).
 */
@Component
public class WeekdayWorkingDayCalculator implements WorkingDayCalculator {

    @Override
    public int count(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Bitiş tarihi başlangıç tarihinden önce olamaz.");
        }
        // datesUntil bitişi dahil etmez; bitiş günü de sayılsın diye bir gün eklenir.
        return (int) startDate.datesUntil(endDate.plusDays(1))
                .filter(this::isWorkingDay)
                .count();
    }

    /** Hafta içi günler iş günüdür; cumartesi ve pazar değildir. */
    protected boolean isWorkingDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> false;
            default -> true;
        };
    }
}
