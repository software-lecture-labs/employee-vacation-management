package com.dogus.vacation.application.usecase.balance.carryover;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.dogus.vacation.application.mediator.RequestValidator;
import com.dogus.vacation.application.mediator.ValidationError;
import org.springframework.stereotype.Component;

/**
 * {@link RunCarryOverRequest} için kural: devir yalnızca geçen yıldan bu yıla yapılabilir.
 *
 * <p>Neden yalnızca biten yıl: yıl bitmeden devredilirse çalışan o yılın kalan günlerinde izin
 * kullanmaya devam edebilir ve aynı günler hem eski yılda kullanılmış hem yeni yıla geçmiş olur.
 * Yıl kapandıktan sonra kalan gün kesinleşmiştir. "Bu yıl" {@link Clock}'tan alınır.
 */
@Component
public class RunCarryOverValidator implements RequestValidator<RunCarryOverRequest> {

    private final Clock clock;

    public RunCarryOverValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<ValidationError> validate(RunCarryOverRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        int thisYear = LocalDate.now(clock).getYear();
        if (request.fromYear() != thisYear - 1) {
            errors.add(new ValidationError("fromYear",
                    "Devir yalnızca geçen yıldan (" + (thisYear - 1) + ") bu yıla (" + thisYear + ") yapılabilir."));
        }
        return errors;
    }
}
