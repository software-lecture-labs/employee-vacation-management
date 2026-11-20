package com.dogus.vacation.application.usecase.leavetype.update;

import java.math.BigDecimal;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * İzin türü güncelleme isteği (FR-12; yalnızca İK).
 *
 * <p>Kod ve "bakiyeden düşer mi" alanı burada yoktur, çünkü oluşturulduktan sonra
 * değişmezler. {@code id} yoldan gelir; Controller gövdeyi {@link #withId(Long)} ile tamamlar.
 *
 * @param id              güncellenecek tür
 * @param name            yeni ad
 * @param annualLimitDays yeni yıllık sınır; boş bırakılabilir, bakiyeden düşmeyen türde boş olmalı
 * @param active          pasif türle yeni talep açılamaz
 */
public record UpdateLeaveTypeRequest(
        @NotNull(message = "İzin türü belirtilmeli.")
        @Positive(message = "Geçerli bir izin türü seçilmeli.")
        Long id,

        @NotBlank(message = "Ad zorunlu.")
        @Size(max = 50, message = "Ad en fazla 50 karakter olabilir.")
        String name,

        @DecimalMin(value = "0", message = "Yıllık sınır negatif olamaz.")
        @DecimalMax(value = "365", message = "Yıllık sınır en fazla 365 gün olabilir.")
        @Digits(integer = 3, fraction = 1, message = "Yıllık sınır en fazla bir ondalık basamak içerebilir.")
        BigDecimal annualLimitDays,

        @NotNull(message = "Aktif olup olmadığı belirtilmeli.")
        Boolean active)
        implements Request<UpdateLeaveTypeResponse> {

    /** Gövdeden gelen isteğe yoldaki id'yi ekler; gövdede id olsa bile yoldaki geçerlidir. */
    public UpdateLeaveTypeRequest withId(Long pathId) {
        return new UpdateLeaveTypeRequest(pathId, name, annualLimitDays, active);
    }
}
