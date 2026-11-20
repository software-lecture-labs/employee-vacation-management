package com.dogus.vacation.application.usecase.leave.submit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.dogus.vacation.application.mediator.ValidationError;
import org.junit.jupiter.api.Test;

/**
 * {@link SubmitLeaveValidator} için birim testleri (BR-06). "Bugün" 2027-01-20 sabit.
 */
class SubmitLeaveValidatorTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2027-01-20T09:00:00Z"), ZoneId.of("Europe/Istanbul"));
    private static final LocalDate TODAY = LocalDate.of(2027, 1, 20);

    private final SubmitLeaveValidator validator = new SubmitLeaveValidator(CLOCK);

    @Test
    void validFutureRangeHasNoErrors() {
        assertThat(validate(LocalDate.of(2027, 2, 5), LocalDate.of(2027, 2, 9))).isEmpty();
    }

    @Test
    void requestStartingTodayIsAllowed() {
        assertThat(validate(TODAY, TODAY)).isEmpty();
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThat(validate(LocalDate.of(2027, 2, 9), LocalDate.of(2027, 2, 5)))
                .containsExactly(new ValidationError("endDate", "Bitiş tarihi başlangıç tarihinden önce olamaz."));
    }

    @Test
    void rangeCrossingYearIsRejected() {
        assertThat(validate(LocalDate.of(2027, 12, 29), LocalDate.of(2028, 1, 3)))
                .extracting(ValidationError::field)
                .containsExactly("endDate");
    }

    @Test
    void pastStartDateIsRejected() {
        assertThat(validate(TODAY.minusDays(1), TODAY.plusDays(2)))
                .containsExactly(new ValidationError("startDate", "Geçmiş bir tarih için izin talebi açılamaz."));
    }

    @Test
    void multipleErrorsAreReturnedTogether() {
        assertThat(validate(TODAY.minusDays(1), TODAY.minusDays(3)))
                .extracting(ValidationError::field)
                .containsExactlyInAnyOrder("endDate", "startDate");
    }

    private List<ValidationError> validate(LocalDate start, LocalDate end) {
        return validator.validate(new SubmitLeaveRequest(1L, start, end, "Aile ziyareti"));
    }
}
