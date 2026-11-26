package com.dogus.vacation.application.usecase.auth.login;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link LoginRequest} için alanlar arası kurallar.
 *
 * <p>Bu istekte alanlar arası kural yoktur: e-posta ve şifrenin biçimi istek üzerindeki
 * anotasyonlarla denetlenir; kullanıcının var olup olmadığı, aktifliği ve şifresi
 * veritabanına baktığı için Handler'dadır.
 *
 * <p>Sınıf, her use case'in dört dosyadan (Request, Response, Handler, Validator) oluşması
 * kuralı (plan 4.2) gereği vardır; yeni bir alanlar arası kural buraya eklenir.
 */
@Component
public class LoginValidator implements RequestValidator<LoginRequest> {

    @Override
    public List<ValidationError> validate(LoginRequest request) {
        return List.of();
    }
}
