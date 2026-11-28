package com.dogus.vacation.application.usecase.cancellation.reject;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link RejectLeaveCancellationRequest} için alanlar arası kurallar. Alanlar arası kural yoktur: alan sınırları istek anotasyonlarıyla
 * denetlenir; durum, tarih (izin başladı mı) ve yetki veritabanına baktığı için Handler'dadır.
 */
@Component
public class RejectLeaveCancellationValidator implements RequestValidator<RejectLeaveCancellationRequest> {

    @Override
    public List<ValidationError> validate(RejectLeaveCancellationRequest request) {
        return List.of();
    }
}
