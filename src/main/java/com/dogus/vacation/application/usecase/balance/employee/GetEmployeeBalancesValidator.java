package com.dogus.vacation.application.usecase.balance.employee;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link GetEmployeeBalancesRequest} için alanlar arası kurallar.
 *
 * <p>Bu istekte alanlar arası kural yoktur: çalışan id'si ve yıl aralığı istek üzerindeki
 * anotasyonlarla denetlenir; çalışanın var olup olmadığı ve isteyenin onu görme yetkisi
 * veritabanına baktığı için {@link GetEmployeeBalancesHandler}'dadır.
 *
 * <p>Sınıf, her use case'in dört dosyadan (Request, Response, Handler, Validator) oluşması
 * kuralı (plan 4.2) gereği vardır; yeni bir alanlar arası kural buraya eklenir.
 */
@Component
public class GetEmployeeBalancesValidator implements RequestValidator<GetEmployeeBalancesRequest> {

    @Override
    public List<ValidationError> validate(GetEmployeeBalancesRequest request) {
        return List.of();
    }
}
