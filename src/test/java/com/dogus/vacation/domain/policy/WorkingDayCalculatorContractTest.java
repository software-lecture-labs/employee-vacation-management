package com.dogus.vacation.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Her {@link WorkingDayCalculator} uygulamasının geçmesi gereken ortak testler (LSP).
 *
 * <p>Yeni bir uygulama (örneğin resmi tatilleri de düşen hesap) bu sınıftan türeyen bir
 * test yazar ve yalnızca {@link #calculator()} metodunu doldurur. Böylece arayüzü kullanan
 * kod (izin talebi açma) hangi uygulamayla çalışırsa çalışsın aynı sonuçları alır.
 * Tarihler Kasım 2026'dan seçildi; bu aralıkta resmi tatil yoktur.
 */
abstract class WorkingDayCalculatorContractTest {

    /** Test edilecek uygulama. */
    protected abstract WorkingDayCalculator calculator();

    @ParameterizedTest(name = "{0} – {1}: {2} iş günü ({3})")
    @CsvSource({
            "2026-11-16, 2026-11-16, 1,  aynı gün hafta içi",
            "2026-11-16, 2026-11-20, 5,  Pazartesi–Cuma",
            "2026-11-20, 2026-11-23, 2,  Cuma–Pazartesi, hafta sonu atlanır",
            "2026-11-21, 2026-11-22, 0,  Cumartesi–Pazar",
            "2026-11-21, 2026-11-21, 0,  aynı gün Cumartesi",
            "2026-11-16, 2026-11-27, 10, iki tam hafta"
    })
    void countsWorkingDaysIncludingStartAndEnd(LocalDate start, LocalDate end, int expected, String description) {
        assertThat(calculator().count(start, end)).isEqualTo(expected);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        LocalDate start = LocalDate.of(2026, 11, 20);
        LocalDate end = LocalDate.of(2026, 11, 16);

        assertThatThrownBy(() -> calculator().count(start, end))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingDates() {
        LocalDate date = LocalDate.of(2026, 11, 16);

        assertThatThrownBy(() -> calculator().count(null, date))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calculator().count(date, null))
                .isInstanceOf(NullPointerException.class);
    }
}
