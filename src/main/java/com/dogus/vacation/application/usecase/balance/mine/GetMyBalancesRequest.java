package com.dogus.vacation.application.usecase.balance.mine;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Giriş yapan çalışanın kendi izin bakiyeleri (FR-32; giriş yapmış herkes).
 *
 * <p>Kimin bakiyesi olduğu istekte yoktur; Handler giriş yapanı {@code CurrentUserProvider}'dan
 * alır. Böylece kimse başkasının id'sini yazarak onun bakiyesini göremez.
 *
 * @param year bakiye yılı, 2000–2100; boşsa bu yıl
 */
public record GetMyBalancesRequest(
        @Min(value = 2000, message = "Yıl 2000'den küçük olamaz.")
        @Max(value = 2100, message = "Yıl 2100'den büyük olamaz.")
        Integer year)
        implements Request<GetMyBalancesResponse> {
}
