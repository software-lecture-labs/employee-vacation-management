package com.dogus.vacation.application.usecase.leavetype.list;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link ListLeaveTypesRequest} için alanlar arası kurallar.
 *
 * <p>İsteğin alanı yoktur; tüm izin türleri listelenir.
 *
 * <p>Sınıf, her use case'in dört dosyadan (Request, Response, Handler, Validator) oluşması
 * kuralı (plan 4.2) gereği vardır; yeni bir alanlar arası kural buraya eklenir.
 */
@Component
public class ListLeaveTypesValidator implements RequestValidator<ListLeaveTypesRequest> {

    @Override
    public List<ValidationError> validate(ListLeaveTypesRequest request) {
        return List.of();
    }
}
