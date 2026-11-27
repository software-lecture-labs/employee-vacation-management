package com.dogus.vacation.application.usecase.employee.deactivate;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Çalışanı pasife alma isteği (FR-08; yalnızca İK). Çalışan silinmez; pasif çalışan giriş
 * yapamaz ve talep açamaz (BR-10).
 *
 * @param id pasife alınacak çalışan
 */
public record DeactivateEmployeeRequest(
        @NotNull(message = "Çalışan belirtilmeli.")
        @Positive(message = "Geçerli bir çalışan seçilmeli.")
        Long id)
        implements Request<DeactivateEmployeeResponse> {
}
