package com.dogus.vacation.application.usecase.cancellation.cancel;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link CancelLeaveRequest} için alanlar arası kurallar. Alanlar arası kural yoktur: gerekçenin boş olmaması ve uzunluğu istek anotasyonlarıyla
 * denetlenir; durum, tarih (izin başladı mı) ve yetki veritabanına baktığı için Handler'dadır.
 */
@Component
public class CancelLeaveValidator implements RequestValidator<CancelLeaveRequest> {

    @Override
    public List<ValidationError> validate(CancelLeaveRequest request) {
        return List.of();
    }
}
