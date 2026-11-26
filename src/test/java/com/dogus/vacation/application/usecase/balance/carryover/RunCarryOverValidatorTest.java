package com.dogus.vacation.application.usecase.balance.carryover;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.dogus.vacation.application.mediator.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link RunCarryOverValidator}: devir yalnızca geçen yıldan bu yıla. */
class RunCarryOverValidatorTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    /** İstanbul'da 4 Ocak 2027: 2026 yılı kapanmış. */
    private final RunCarryOverValidator validator =
            new RunCarryOverValidator(Clock.fixed(Instant.parse("2027-01-04T06:00:00Z"), ISTANBUL));

    @Test
    void acceptsLastYear() {
        assertThat(validator.validate(new RunCarryOverRequest(2026))).isEmpty();
    }

    /** 2027 bitmemiş bir yıl: kalanı henüz kesin değil. 2025 ise zaten bir kez devredilmiş olmalı. */
    @ParameterizedTest
    @ValueSource(ints = {2025, 2027, 2028})
    void rejectsAnyOtherYear(int fromYear) {
        assertThat(validator.validate(new RunCarryOverRequest(fromYear)))
                .containsExactly(new ValidationError("fromYear",
                        "Devir yalnızca geçen yıldan (2026) bu yıla (2027) yapılabilir."));
    }

    /** Aralık sonunda yıl henüz bitmemişse o yılın günleri devredilemez. */
    @Test
    void theYearMustHaveEndedInTheClocksTimeZone() {
        RunCarryOverValidator onNewYearsEve =
                new RunCarryOverValidator(Clock.fixed(Instant.parse("2026-12-31T20:00:00Z"), ISTANBUL));

        assertThat(onNewYearsEve.validate(new RunCarryOverRequest(2026))).hasSize(1);
        assertThat(onNewYearsEve.validate(new RunCarryOverRequest(2025))).isEmpty();
    }
}
