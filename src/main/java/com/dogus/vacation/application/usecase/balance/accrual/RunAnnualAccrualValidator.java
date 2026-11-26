package com.dogus.vacation.application.usecase.balance.accrual;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link RunAnnualAccrualRequest} için kural: hak ediş yalnızca bu yıl veya gelecek yıl için
 * çalıştırılabilir.
 *
 * <p>Gelecek yıla izin verilir, çünkü İK yeni yılın bakiyelerini Aralık'ta hazırlayabilir.
 * Daha ileri ya da geçmiş bir yıl büyük olasılıkla yazım hatasıdır (2026 yerine 2062 gibi) ve
 * BR-11 gereği o yıl bir daha çalıştırılamayacağı için baştan reddedilir. "Bu yıl" sabit
 * yazılmaz, {@link Clock}'tan alınır; testler sabit bir saat verir.
 */
@Component
public class RunAnnualAccrualValidator implements RequestValidator<RunAnnualAccrualRequest> {

    private final Clock clock;

    public RunAnnualAccrualValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<ValidationError> validate(RunAnnualAccrualRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        int thisYear = LocalDate.now(clock).getYear();
        int year = request.year();
        if (year != thisYear && year != thisYear + 1) {
            errors.add(new ValidationError("year",
                    "Hak ediş yalnızca bu yıl (" + thisYear + ") veya gelecek yıl (" + (thisYear + 1)
                            + ") için çalıştırılabilir."));
        }
        return errors;
    }
}
