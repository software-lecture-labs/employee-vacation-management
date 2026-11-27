package com.dogus.vacation.application.usecase.employee.list;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link ListEmployeesRequest} için alanlar arası kurallar.
 *
 * <p>Alanlar arası kural yoktur: filtreler birbirinden bağımsızdır, sayfalama sınırları istek
 * anotasyonlarıyla denetlenir. Olmayan bir departmanla filtrelemek hata değildir, boş sayfa döner.
 */
@Component
public class ListEmployeesValidator implements RequestValidator<ListEmployeesRequest> {

    @Override
    public List<ValidationError> validate(ListEmployeesRequest request) {
        return List.of();
    }
}
