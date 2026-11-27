package com.dogus.vacation.application.usecase.department.update;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link UpdateDepartmentRequest} için alanlar arası kurallar.
 *
 * <p>Alanlar arası kural yoktur; adın tekilliği ve departmanın varlığı veritabanına baktığı
 * için Handler'da denetlenir.
 */
@Component
public class UpdateDepartmentValidator implements RequestValidator<UpdateDepartmentRequest> {

    @Override
    public List<ValidationError> validate(UpdateDepartmentRequest request) {
        return List.of();
    }
}
