package com.dogus.vacation.application.usecase.leavetype.create;

import java.math.BigDecimal;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Yeni izin türü ekleme isteği (FR-12; yalnızca İK).
 *
 * @param code               değişmez kod; büyük harf ve alt çizgi, 2–30 karakter (MARRIAGE_LEAVE)
 * @param name               görünen ad, en fazla 50 karakter
 * @param deductsFromBalance bakiyeden düşer mi; sonradan değiştirilemez
 * @param annualLimitDays    yıllık üst sınır, 0–365; bakiyeden düşmeyen türde boş olmalı
 */
public record CreateLeaveTypeRequest(
        @NotBlank(message = "Kod zorunlu.")
        @Pattern(regexp = "[A-Z][A-Z_]{1,29}",
                message = "Kod büyük harfle başlamalı; yalnızca büyük harf ve alt çizgi içerebilir (2–30 karakter).")
        String code,

        @NotBlank(message = "Ad zorunlu.")
        @Size(max = 50, message = "Ad en fazla 50 karakter olabilir.")
        String name,

        @NotNull(message = "Bakiyeden düşüp düşmediği belirtilmeli.")
        Boolean deductsFromBalance,

        @DecimalMin(value = "0", message = "Yıllık sınır negatif olamaz.")
        @DecimalMax(value = "365", message = "Yıllık sınır en fazla 365 gün olabilir.")
        @Digits(integer = 3, fraction = 1, message = "Yıllık sınır en fazla bir ondalık basamak içerebilir.")
        BigDecimal annualLimitDays)
        implements Request<CreateLeaveTypeResponse> {
}
