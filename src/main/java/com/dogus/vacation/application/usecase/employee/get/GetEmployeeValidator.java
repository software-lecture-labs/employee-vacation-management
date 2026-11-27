package com.dogus.vacation.application.usecase.employee.get;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link GetEmployeeRequest} için alanlar arası kurallar. Tek alan olduğu için kural yoktur;
 * çalışanın varlığı veritabanına baktığı için Handler'da denetlenir.
 */
@Component
public class GetEmployeeValidator implements RequestValidator<GetEmployeeRequest> {

    @Override
    public List<ValidationError> validate(GetEmployeeRequest request) {
        return List.of();
    }
}
