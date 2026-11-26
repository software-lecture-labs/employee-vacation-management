package com.dogus.vacation.application.usecase.balance.employee;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Bir çalışanın izin bakiyeleri (FR-32; yönetici yalnızca kendi ekibini, İK herkesi görür).
 *
 * @param employeeId bakiyesi istenen çalışan (yoldan gelir)
 * @param year       bakiye yılı, 2000–2100; boşsa bu yıl
 */
public record GetEmployeeBalancesRequest(
        @NotNull(message = "Çalışan belirtilmeli.")
        @Positive(message = "Geçerli bir çalışan seçilmeli.")
        Long employeeId,

        @Min(value = 2000, message = "Yıl 2000'den küçük olamaz.")
        @Max(value = 2100, message = "Yıl 2100'den büyük olamaz.")
        Integer year)
        implements Request<GetEmployeeBalancesResponse> {
}
