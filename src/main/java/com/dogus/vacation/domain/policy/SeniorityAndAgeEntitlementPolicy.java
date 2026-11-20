package com.dogus.vacation.domain.policy;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * Kıdem ve yaşa göre yıllık izin hakkı (FR-33; gereksinim dokümanı bölüm 4.1).
 *
 * <p>Kıdem ve yaş, hak ediş yılından işe giriş ve doğum yılı çıkarılarak bulunur;
 * ay ve gün dikkate alınmaz (2027 hak edişinde 2021'de giren biri 6 yıl sayılır).
 * Bu, 4857 sayılı İş Kanunu'nun 53. maddesinin projeye özel basitleştirilmiş hâlidir.
 *
 * <table>
 *   <caption>Kıdeme göre hak</caption>
 *   <tr><th>Kıdem</th><th>Yıllık izin</th></tr>
 *   <tr><td>1 yıldan az</td><td>Hak yok (0)</td></tr>
 *   <tr><td>1–5 yıl</td><td>14 gün</td></tr>
 *   <tr><td>6–14 yıl</td><td>20 gün</td></tr>
 *   <tr><td>15 yıl ve üstü</td><td>26 gün</td></tr>
 * </table>
 *
 * <p>O yıl 18 yaşında veya küçük ya da 50 yaşında veya büyük olanlar için hak 20 günden
 * az olamaz. Yaş kuralı yalnızca hakkı olan çalışana uygulanır: 1 yılını doldurmamış
 * çalışanın yaşı ne olursa olsun hakkı yoktur (kanunda da en az 1 yıl çalışma şartı
 * yaştan önce gelir).
 */
@Component
public class SeniorityAndAgeEntitlementPolicy implements EntitlementPolicy {

    private static final int NO_ENTITLEMENT = 0;
    private static final int JUNIOR_DAYS = 14;
    private static final int MID_DAYS = 20;
    private static final int SENIOR_DAYS = 26;

    private static final int MID_FROM_YEARS = 6;
    private static final int SENIOR_FROM_YEARS = 15;

    private static final int YOUNG_MAX_AGE = 18;
    private static final int OLDER_MIN_AGE = 50;
    private static final int AGE_PROTECTED_MIN_DAYS = 20;

    @Override
    public int annualLeaveDays(LocalDate hireDate, LocalDate birthDate, int year) {
        Objects.requireNonNull(hireDate, "hireDate");
        Objects.requireNonNull(birthDate, "birthDate");

        int seniority = year - hireDate.getYear();
        if (seniority < 0) {
            throw new IllegalArgumentException("Hak ediş yılı (" + year
                    + ") işe giriş yılından (" + hireDate.getYear() + ") önce olamaz.");
        }
        if (seniority == 0) {
            return NO_ENTITLEMENT;
        }

        int days = daysForSeniority(seniority);
        int age = year - birthDate.getYear();
        return isAgeProtected(age) ? Math.max(days, AGE_PROTECTED_MIN_DAYS) : days;
    }

    /** Kıdem tablosu; {@code seniority} en az 1'dir. */
    private static int daysForSeniority(int seniority) {
        if (seniority >= SENIOR_FROM_YEARS) {
            return SENIOR_DAYS;
        }
        if (seniority >= MID_FROM_YEARS) {
            return MID_DAYS;
        }
        return JUNIOR_DAYS;
    }

    /** 18 yaşında veya küçük ya da 50 yaşında veya büyük. */
    private static boolean isAgeProtected(int age) {
        return age <= YOUNG_MAX_AGE || age >= OLDER_MIN_AGE;
    }
}
