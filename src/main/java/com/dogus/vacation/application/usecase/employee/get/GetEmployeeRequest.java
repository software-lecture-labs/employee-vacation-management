package com.dogus.vacation.application.usecase.employee.get;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Bir çalışanın detayı (FR-09; yalnızca İK).
 *
 * @param id çalışan id'si
 */
public record GetEmployeeRequest(
        @NotNull(message = "Çalışan belirtilmeli.")
        @Positive(message = "Geçerli bir çalışan seçilmeli.")
        Long id)
        implements Request<GetEmployeeResponse> {
}
