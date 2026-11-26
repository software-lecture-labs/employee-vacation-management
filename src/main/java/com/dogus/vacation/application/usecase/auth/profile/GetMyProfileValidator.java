package com.dogus.vacation.application.usecase.auth.profile;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link GetMyProfileRequest} için alanlar arası kurallar.
 *
 * <p>İsteğin alanı yoktur: kimin profili olduğu token'dan gelir ({@code CurrentUserProvider}).
 *
 * <p>Sınıf, her use case'in dört dosyadan (Request, Response, Handler, Validator) oluşması
 * kuralı (plan 4.2) gereği vardır; yeni bir alanlar arası kural buraya eklenir.
 */
@Component
public class GetMyProfileValidator implements RequestValidator<GetMyProfileRequest> {

    @Override
    public List<ValidationError> validate(GetMyProfileRequest request) {
        return List.of();
    }
}
