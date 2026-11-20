package com.dogus.vacation.application.usecase.history.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.dogus.vacation.application.mediator.ValidationError;
import org.junit.jupiter.api.Test;

/** Tarih aralığı kuralı ve sayfalama varsayılanları; Spring gerekmez. */
class SearchLeaveHistoryValidatorTest {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    private final SearchLeaveHistoryValidator validator = new SearchLeaveHistoryValidator();

    private static SearchLeaveHistoryRequest between(LocalDate from, LocalDate to) {
        return new SearchLeaveHistoryRequest(from, to, null, null, null, null, null);
    }

    @Test
    void acceptsAnOrderedRangeASingleDayOrAnOpenRange() {
        assertThat(validator.validate(between(NOV_1, NOV_30))).isEmpty();
        assertThat(validator.validate(between(NOV_1, NOV_1))).isEmpty();
        assertThat(validator.validate(between(NOV_1, null))).isEmpty();
        assertThat(validator.validate(between(null, NOV_30))).isEmpty();
    }

    @Test
    void rejectsAReversedRange() {
        assertThat(validator.validate(between(NOV_30, NOV_1)))
                .containsExactly(new ValidationError("to", SearchLeaveHistoryValidator.REVERSED_RANGE));
    }

    @Test
    void usesTheFirstPageOfTwentyWhenPagingIsNotGiven() {
        SearchLeaveHistoryRequest request = between(null, null);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(SearchLeaveHistoryRequest.DEFAULT_SIZE).isEqualTo(20);
    }
}
