package com.dogus.vacation.domain.policy;

import java.time.LocalDate;

/**
 * İki tarih arasındaki iş günü sayısını hesaplar (FR-15, BR-01).
 *
 * <p>Başlangıç ve bitiş günleri dahildir. Hangi günlerin iş günü sayıldığı
 * uygulamaya bırakılır: {@link WeekdayWorkingDayCalculator} yalnızca hafta sonunu
 * düşer; resmi tatilleri de düşen bir uygulama (bonus, FR-38) aynı arayüzü uygular.
 * Her uygulama {@code WorkingDayCalculatorContractTest}'ten geçmelidir.
 */
public interface WorkingDayCalculator {

    /**
     * @param startDate izin başlangıcı (dahil)
     * @param endDate   izin bitişi (dahil)
     * @return aralıktaki iş günü sayısı; hepsi tatilse 0
     * @throws NullPointerException     tarihlerden biri boşsa
     * @throws IllegalArgumentException bitiş başlangıçtan önceyse
     */
    int count(LocalDate startDate, LocalDate endDate);
}
