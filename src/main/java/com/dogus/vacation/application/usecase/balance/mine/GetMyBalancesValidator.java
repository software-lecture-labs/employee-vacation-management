package com.dogus.vacation.application.usecase.balance.mine;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link GetMyBalancesRequest} için alanlar arası kurallar.
 *
 * <p>Bu istekte alanlar arası kural yoktur: yıl aralığı tek alanlık bir kuraldır ve istek
 * üzerindeki anotasyonlarla denetlenir.
 *
 * <p>Sınıf, her use case'in dört dosyadan (Request, Response, Handler, Validator) oluşması
 * kuralı (plan 4.2) gereği vardır; yeni bir alanlar arası kural buraya eklenir.
 */
@Component
public class GetMyBalancesValidator implements RequestValidator<GetMyBalancesRequest> {

    @Override
    public List<ValidationError> validate(GetMyBalancesRequest request) {
        return List.of();
    }
}
