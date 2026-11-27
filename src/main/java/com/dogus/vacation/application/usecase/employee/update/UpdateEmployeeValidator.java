package com.dogus.vacation.application.usecase.employee.update;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import com.dogus.vacation.application.usecase.employee.EmployeeDateRules;
import org.springframework.stereotype.Component;

/**
 * {@link UpdateEmployeeRequest} için kurallar; veritabanına bakmaz.
 * <ul>
 *   <li>Tarih kuralları eklemeyle aynıdır ({@link EmployeeDateRules}).</li>
 *   <li>Çalışan kendi yöneticisi olamaz.</li>
 * </ul>
 * E-postanın tekilliği, departmanın ve yöneticinin varlığı, yönetici döngüsü ve BR-13 veritabanına
 * baktığı için Handler'dadır.
 */
@Component
public class UpdateEmployeeValidator implements RequestValidator<UpdateEmployeeRequest> {

    private final Clock clock;

    public UpdateEmployeeValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<ValidationError> validate(UpdateEmployeeRequest request) {
        List<ValidationError> dateErrors =
                EmployeeDateRules.check(request.hireDate(), request.birthDate(), LocalDate.now(clock));
        List<ValidationError> managerErrors = request.id().equals(request.managerId())
                ? List.of(new ValidationError("managerId", "Çalışan kendi yöneticisi olamaz."))
                : List.of();
        return Stream.concat(dateErrors.stream(), managerErrors.stream()).toList();
    }
}
