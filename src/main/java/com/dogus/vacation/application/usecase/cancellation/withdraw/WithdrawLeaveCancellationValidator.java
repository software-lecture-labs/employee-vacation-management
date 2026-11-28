package com.dogus.vacation.application.usecase.cancellation.withdraw;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link WithdrawLeaveCancellationRequest} için alanlar arası kurallar. Alanlar arası kural yoktur: alan sınırları istek anotasyonlarıyla
 * denetlenir; durum, tarih (izin başladı mı) ve yetki veritabanına baktığı için Handler'dadır.
 */
@Component
public class WithdrawLeaveCancellationValidator implements RequestValidator<WithdrawLeaveCancellationRequest> {

    @Override
    public List<ValidationError> validate(WithdrawLeaveCancellationRequest request) {
        return List.of();
    }
}
