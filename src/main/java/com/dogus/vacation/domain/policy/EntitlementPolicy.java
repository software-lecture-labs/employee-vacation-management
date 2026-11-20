package com.dogus.vacation.domain.policy;

import java.time.LocalDate;

/**
 * Bir çalışanın verilen yıl için yıllık izin hakkını hesaplar (FR-33).
 *
 * <p>Kuralın kendisi uygulamaya bırakılır: {@link SeniorityAndAgeEntitlementPolicy}
 * gereksinim dokümanının 4.1 bölümündeki kıdem ve yaş tablosunu uygular. Kural
 * değişirse (örneğin şirkete özel ek günler) yeni bir uygulama yazılır; hakkı kullanan
 * kod (çalışan ekleme T4, yıllık hak ediş T3) değişmez.
 */
public interface EntitlementPolicy {

    /**
     * @param hireDate  işe giriş tarihi; kıdem yalnızca yılına bakılarak bulunur
     * @param birthDate doğum tarihi; yaş yalnızca yılına bakılarak bulunur
     * @param year      hakkın hesaplandığı yıl
     * @return o yıl için yıllık izin günü; hak yoksa 0
     * @throws NullPointerException     tarihlerden biri boşsa
     * @throws IllegalArgumentException işe giriş yılı hak ediş yılından sonraysa
     */
    int annualLeaveDays(LocalDate hireDate, LocalDate birthDate, int year);
}
