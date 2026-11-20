package com.dogus.vacation.application.usecase.history.search;

import java.util.ArrayList;
import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/** {@link SearchLeaveHistoryRequest} için alanlar arası kural: tarih aralığı ters olamaz. */
@Component
public class SearchLeaveHistoryValidator implements RequestValidator<SearchLeaveHistoryRequest> {

    static final String REVERSED_RANGE = "Bitiş tarihi başlangıç tarihinden önce olamaz.";

    @Override
    public List<ValidationError> validate(SearchLeaveHistoryRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request.from() != null && request.to() != null && request.to().isBefore(request.from())) {
            errors.add(new ValidationError("to", REVERSED_RANGE));
        }
        return errors;
    }
}
