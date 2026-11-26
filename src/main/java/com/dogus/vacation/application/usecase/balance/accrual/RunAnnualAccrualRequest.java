package com.dogus.vacation.application.usecase.balance.accrual;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.NotNull;

/**
 * Bir yıl için bütün aktif çalışanlara yıllık bakiye açar (T3; FR-33, FR-34, BR-11; yalnızca İK).
 *
 * <p>İşlemi yapan İK istekte yoktur; Handler onu {@code CurrentUserProvider}'dan alır.
 * Hangi yılların kabul edildiği {@link RunAnnualAccrualValidator}'dadır.
 *
 * @param year hak ediş yılı; zorunlu
 */
public record RunAnnualAccrualRequest(
        @NotNull(message = "Yıl zorunludur.")
        Integer year)
        implements Request<RunAnnualAccrualResponse> {
}
