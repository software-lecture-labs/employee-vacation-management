package com.dogus.vacation.application.usecase.leavetype.create;

import java.util.ArrayList;
import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link CreateLeaveTypeRequest} için alanlar arası kural: bakiyeden düşmeyen türde
 * (hastalık, ücretsiz izin gibi) yıllık sınır anlamsızdır, boş olmalıdır.
 * Kod ve adın tekilliği veritabanına baktığı için Handler'dadır.
 */
@Component
public class CreateLeaveTypeValidator implements RequestValidator<CreateLeaveTypeRequest> {

    static final String LIMIT_WITHOUT_BALANCE = "Bakiyeden düşmeyen türde yıllık sınır boş olmalı.";

    @Override
    public List<ValidationError> validate(CreateLeaveTypeRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (!request.deductsFromBalance() && request.annualLimitDays() != null) {
            errors.add(new ValidationError("annualLimitDays", LIMIT_WITHOUT_BALANCE));
        }
        return errors;
    }
}
