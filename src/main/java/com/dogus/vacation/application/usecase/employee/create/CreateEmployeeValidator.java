package com.dogus.vacation.application.usecase.employee.create;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.usecase.employee.EmployeeDateRules;
import org.springframework.stereotype.Component;

/**
 * {@link CreateEmployeeRequest} için tarih kuralları; veritabanına bakmaz.
 * Kurallar güncellemeyle ortaktır ({@link EmployeeDateRules}).
 *
 * <p>E-postanın tekilliği, departmanın ve yöneticinin varlığı veritabanına baktığı için
 * Handler'dadır.
 */
@Component
public class CreateEmployeeValidator implements RequestValidator<CreateEmployeeRequest> {

    private final Clock clock;

    public CreateEmployeeValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<ValidationError> validate(CreateEmployeeRequest request) {
        return EmployeeDateRules.check(request.hireDate(), request.birthDate(), LocalDate.now(clock));
    }
}
