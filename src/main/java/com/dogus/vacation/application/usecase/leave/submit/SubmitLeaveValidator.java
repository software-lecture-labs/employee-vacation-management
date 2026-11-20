package com.dogus.vacation.application.usecase.leave.submit;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * İzin talebinin tarih kuralları (BR-06, plan K-2). Veritabanına bakmaz.
 *
 * <ul>
 *   <li>Bitiş başlangıçtan önce olamaz.</li>
 *   <li>Başlangıç ve bitiş aynı takvim yılında olmalı; yılı aşan izin iki talep olarak açılır.</li>
 *   <li>Başlangıç bugünden önce olamaz (geçmiş tarihe talep yok).</li>
 * </ul>
 *
 * <p>İş günü sayısının 0'dan büyük ve en fazla 30 olması, izin türünün aktifliği, çakışma
 * ve bakiye veritabanı veya hesap gerektirdiği için Handler'dadır.
 */
@Component
public class SubmitLeaveValidator implements RequestValidator<SubmitLeaveRequest> {

    private final Clock clock;

    public SubmitLeaveValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<ValidationError> validate(SubmitLeaveRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();

        if (end.isBefore(start)) {
            errors.add(new ValidationError("endDate", "Bitiş tarihi başlangıç tarihinden önce olamaz."));
        } else if (start.getYear() != end.getYear()) {
            errors.add(new ValidationError("endDate",
                    "Başlangıç ve bitiş aynı takvim yılında olmalı; yılı aşan izin için iki talep açın."));
        }
        if (start.isBefore(LocalDate.now(clock))) {
            errors.add(new ValidationError("startDate", "Geçmiş bir tarih için izin talebi açılamaz."));
        }
        return errors;
    }
}
