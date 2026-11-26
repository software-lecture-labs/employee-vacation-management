package com.dogus.vacation.application.usecase.balance.accrual;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.dogus.vacation.application.mediator.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link RunAnnualAccrualValidator}: yalnızca bu yıl ve gelecek yıl kabul edilir. */
class RunAnnualAccrualValidatorTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    /** İstanbul'da 26 Kasım 2026. */
    private final RunAnnualAccrualValidator validator =
            new RunAnnualAccrualValidator(Clock.fixed(Instant.parse("2026-11-26T09:00:00Z"), ISTANBUL));

    @ParameterizedTest
    @ValueSource(ints = {2026, 2027})
    void acceptsThisYearAndNextYear(int year) {
        assertThat(validator.validate(new RunAnnualAccrualRequest(year))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {2025, 2028, 2062})
    void rejectsAnyOtherYear(int year) {
        assertThat(validator.validate(new RunAnnualAccrualRequest(year)))
                .containsExactly(new ValidationError("year",
                        "Hak ediş yalnızca bu yıl (2026) veya gelecek yıl (2027) için çalıştırılabilir."));
    }

    /** UTC'de hâlâ 31 Aralık 2026 iken İstanbul'da yeni yıl başlamıştır; "bu yıl" saatin bölgesinden alınır. */
    @Test
    void thisYearComesFromTheClocksTimeZone() {
        RunAnnualAccrualValidator atNewYear =
                new RunAnnualAccrualValidator(Clock.fixed(Instant.parse("2026-12-31T22:30:00Z"), ISTANBUL));

        assertThat(atNewYear.validate(new RunAnnualAccrualRequest(2026))).hasSize(1);
        assertThat(atNewYear.validate(new RunAnnualAccrualRequest(2028))).isEmpty();
    }
}
