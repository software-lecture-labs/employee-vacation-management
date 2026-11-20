package com.dogus.vacation.application.common.view;

import java.math.BigDecimal;

/**
 * Bir izin türünün tüm alanları (izin türü listesi, ekleme ve güncelleme cevapları).
 *
 * @param id                 izin türü id'si
 * @param code               değişmez kod (ANNUAL, EXCUSE, SICK, UNPAID ...)
 * @param name               görünen ad
 * @param deductsFromBalance bakiyeden düşer mi (BR-03); oluşturulduktan sonra değişmez
 * @param annualLimitDays    yıllık üst sınır; boşsa sınır yok ya da kıdemle hesaplanıyor
 * @param active             pasif türle yeni talep açılamaz
 */
public record LeaveTypeView(
        Long id,
        String code,
        String name,
        boolean deductsFromBalance,
        BigDecimal annualLimitDays,
        boolean active) {
}
