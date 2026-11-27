package com.dogus.vacation.application.usecase.employee.deactivate;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link DeactivateEmployeeRequest} için alanlar arası kurallar. Tek alan olduğu için kural yoktur;
 * çalışanın varlığı ve BR-13 veritabanına baktığı için Handler'dadır.
 */
@Component
public class DeactivateEmployeeValidator implements RequestValidator<DeactivateEmployeeRequest> {

    @Override
    public List<ValidationError> validate(DeactivateEmployeeRequest request) {
        return List.of();
    }
}
