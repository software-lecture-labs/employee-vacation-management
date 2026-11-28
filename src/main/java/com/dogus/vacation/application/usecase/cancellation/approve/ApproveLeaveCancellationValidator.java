package com.dogus.vacation.application.usecase.cancellation.approve;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link ApproveLeaveCancellationRequest} için alanlar arası kurallar. Alanlar arası kural yoktur: alan sınırları istek anotasyonlarıyla
 * denetlenir; durum, tarih (izin başladı mı) ve yetki veritabanına baktığı için Handler'dadır.
 */
@Component
public class ApproveLeaveCancellationValidator implements RequestValidator<ApproveLeaveCancellationRequest> {

    @Override
    public List<ValidationError> validate(ApproveLeaveCancellationRequest request) {
        return List.of();
    }
}
