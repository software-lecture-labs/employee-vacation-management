package com.dogus.vacation.application.mediator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * {@link ValidationException} ve {@link ValidationError} için birim testleri.
 * Spring gerekmez.
 */
class ValidationExceptionTest {

    @Test
    void keepsErrorsAndUsesTurkishMessage() {
        ValidationError error = new ValidationError("reason", "Gerekçe boş olamaz.");

        ValidationException exception = new ValidationException(List.of(error));

        assertThat(exception.getErrors()).containsExactly(error);
        assertThat(exception.getMessage()).isEqualTo("İstek geçersiz.");
    }

    @Test
    void errorListCannotBeChangedFromOutside() {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError("startDate", "Başlangıç tarihi zorunlu."));
        ValidationException exception = new ValidationException(errors);

        errors.add(new ValidationError("endDate", "Bitiş tarihi zorunlu."));

        assertThat(exception.getErrors()).hasSize(1);
        assertThatThrownBy(() -> exception.getErrors().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsEmptyOrMissingErrorList() {
        List<ValidationError> empty = List.of();

        assertThatThrownBy(() -> new ValidationException(empty))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ValidationException(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validationErrorRequiresFieldAndMessage() {
        assertThatThrownBy(() -> new ValidationError(null, "Mesaj"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ValidationError("field", null))
                .isInstanceOf(NullPointerException.class);
    }
}
