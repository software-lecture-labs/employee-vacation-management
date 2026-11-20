package com.dogus.vacation.application.usecase.leavetype.create;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.dogus.vacation.application.mediator.ValidationError;
import org.junit.jupiter.api.Test;

/** {@link CreateLeaveTypeValidator}'ın alanlar arası kuralı; Spring gerekmez. */
class CreateLeaveTypeValidatorTest {

    private final CreateLeaveTypeValidator validator = new CreateLeaveTypeValidator();

    @Test
    void acceptsALimitForATypeThatUsesTheBalance() {
        CreateLeaveTypeRequest request =
                new CreateLeaveTypeRequest("MARRIAGE_LEAVE", "Evlilik izni", true, new BigDecimal("3"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsNoLimitForATypeThatDoesNotUseTheBalance() {
        CreateLeaveTypeRequest request = new CreateLeaveTypeRequest("BLOOD_DONATION", "Kan bağışı", false, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsALimitForATypeThatDoesNotUseTheBalance() {
        CreateLeaveTypeRequest request =
                new CreateLeaveTypeRequest("BLOOD_DONATION", "Kan bağışı", false, BigDecimal.ONE);

        assertThat(validator.validate(request)).containsExactly(
                new ValidationError("annualLimitDays", CreateLeaveTypeValidator.LIMIT_WITHOUT_BALANCE));
    }
}
