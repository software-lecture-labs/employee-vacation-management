package com.dogus.vacation.application.usecase.leavetype.update;

import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link UpdateLeaveTypeRequest} için alanlar arası kurallar.
 *
 * <p>Bu istekte alanlar arası kural yoktur. "Bakiyeden düşmeyen türde yıllık sınır boş
 * olmalı" kuralı türün kayıtlı halini bilmeyi gerektirdiği için
 * {@link UpdateLeaveTypeHandler}'dadır.
 *
 * <p>Sınıf, her use case'in dört dosyadan (Request, Response, Handler, Validator) oluşması
 * kuralı (plan 4.2) gereği vardır; yeni bir alanlar arası kural buraya eklenir.
 */
@Component
public class UpdateLeaveTypeValidator implements RequestValidator<UpdateLeaveTypeRequest> {

    @Override
    public List<ValidationError> validate(UpdateLeaveTypeRequest request) {
        return List.of();
    }
}
