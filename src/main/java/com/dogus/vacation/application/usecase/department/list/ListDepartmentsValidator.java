package com.dogus.vacation.application.usecase.department.list;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link ListDepartmentsRequest} için alanlar arası kurallar.
 *
 * <p>Bu istekte alanlar arası kural yoktur: sayfa ve sayfa büyüklüğü sınırları tek
 * alanlık kurallardır ve istek üzerindeki anotasyonlarla denetlenir. Sınıf, her use
 * case'in dört dosyadan oluşması kuralı (plan 4.2) ve ileride eklenecek filtreler
 * için yerini korur.
 */
@Component
public class ListDepartmentsValidator implements RequestValidator<ListDepartmentsRequest> {

    @Override
    public List<ValidationError> validate(ListDepartmentsRequest request) {
        return List.of();
    }
}
