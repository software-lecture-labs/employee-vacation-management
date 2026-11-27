package com.dogus.vacation.application.usecase.department.create;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link CreateDepartmentRequest} için alanlar arası kurallar.
 *
 * <p>Tek alan olduğu için alanlar arası kural yoktur; ad zorunluluğu ve uzunluğu istek
 * anotasyonlarıyla, adın tekilliği veritabanına baktığı için Handler'da denetlenir.
 */
@Component
public class CreateDepartmentValidator implements RequestValidator<CreateDepartmentRequest> {

    @Override
    public List<ValidationError> validate(CreateDepartmentRequest request) {
        return List.of();
    }
}
