package com.dogus.vacation.application.common.view;

import java.math.BigDecimal;

/**
 * Bir izin türündeki yıllık bakiye (FR-32).
 *
 * @param leaveType     izin türü
 * @param year          bakiye yılı
 * @param entitledDays  hak edilen gün
 * @param usedDays      kullanılan gün
 * @param remainingDays kalan gün (hak edilen - kullanılan)
 */
public record BalanceView(
        LeaveTypeRef leaveType,
        int year,
        BigDecimal entitledDays,
        BigDecimal usedDays,
        BigDecimal remainingDays) {
}
