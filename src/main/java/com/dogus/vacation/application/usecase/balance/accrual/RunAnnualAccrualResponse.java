package com.dogus.vacation.application.usecase.balance.accrual;

import java.time.OffsetDateTime;

/**
 * Yıllık hak edişin özeti.
 *
 * @param year              hak ediş yılı
 * @param accruedEmployees  en az bir bakiyesi açılan çalışan sayısı; çalıştırma kaydına da yazılır
 * @param skippedEmployees  hiçbir bakiyesi açılmayan aktif çalışan sayısı (o yılın bakiyeleri zaten
 *                          açılmış ya da henüz işe başlamamış)
 * @param openedBalances    açılan bakiye satırı sayısı (bir çalışana birden çok tür açılabilir)
 * @param runAt             çalıştırma zamanı
 */
public record RunAnnualAccrualResponse(int year, int accruedEmployees, int skippedEmployees,
                                       int openedBalances, OffsetDateTime runAt) {
}
