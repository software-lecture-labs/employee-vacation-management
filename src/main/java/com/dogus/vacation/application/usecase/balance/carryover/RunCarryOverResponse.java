package com.dogus.vacation.application.usecase.balance.carryover;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Devrin özeti.
 *
 * @param fromYear         günleri devredilen yıl
 * @param toYear           günlerin eklendiği yıl
 * @param carriedEmployees kalan günü devredilen çalışan sayısı
 * @param carriedDays      devredilen toplam gün
 * @param skippedEmployees kalan günü olduğu hâlde yeni yıl bakiyesi bulunmayan çalışan sayısı
 * @param runAt            çalıştırma zamanı
 */
public record RunCarryOverResponse(int fromYear, int toYear, int carriedEmployees, BigDecimal carriedDays,
                                   int skippedEmployees, OffsetDateTime runAt) {
}
