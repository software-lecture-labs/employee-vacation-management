package com.dogus.vacation.application.usecase.cancellation.request;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link RequestLeaveCancellationRequest} için alanlar arası kurallar. Alanlar arası kural yoktur: gerekçenin boş olmaması ve uzunluğu istek anotasyonlarıyla
 * denetlenir; durum, tarih (izin başladı mı) ve yetki veritabanına baktığı için Handler'dadır.
 */
@Component
public class RequestLeaveCancellationValidator implements RequestValidator<RequestLeaveCancellationRequest> {

    @Override
    public List<ValidationError> validate(RequestLeaveCancellationRequest request) {
        return List.of();
    }
}
