package com.dogus.vacation.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Gereksinim dokümanı 4.1'deki kıdem ve yaş tablosunu satır satır doğrular.
 *
 * <p>Hak ediş yılı her durumda 2027'dir; kıdem ve yaş yalnızca yıl farkıyla bulunduğu
 * için işe giriş ve doğum tarihlerinin ay ve günü sonucu değiştirmez. Spring gerekmez:
 * saf birim testi.
 */
class SeniorityAndAgeEntitlementPolicyTest {

    private static final int YEAR = 2027;

    private final EntitlementPolicy policy = new SeniorityAndAgeEntitlementPolicy();

    @ParameterizedTest(name = "işe giriş {0}, doğum {1}: {2} gün ({3})")
    @CsvSource({
            // Kıdem tablosu (yaş 30–40, kuralın dışında)
            "2027-03-01, 1997-05-10, 0,  0 yıl: hak yok",
            "2026-12-31, 1997-05-10, 14, 1 yıl: ay ve gün dikkate alınmaz",
            "2022-06-15, 1990-01-01, 14, 5 yıl",
            "2021-01-04, 1990-01-01, 20, 6 yıl (dokümandaki örnek)",
            "2013-09-01, 1987-01-01, 20, 14 yıl",
            "2012-09-01, 1987-01-01, 26, 15 yıl",
            "2000-02-01, 1975-01-01, 26, 27 yıl, yaş kuralı 26'yı düşürmez",
            // Yaş kuralı: 18 ve altı, 50 ve üstü en az 20 gün
            "2025-04-01, 1976-08-20, 20, 51 yaş + 2 yıl (dokümandaki örnek)",
            "2025-04-01, 1977-12-31, 20, 50 yaş sınırı dahil",
            "2025-04-01, 1978-01-01, 14, 49 yaş: kural uygulanmaz",
            "2026-07-01, 2009-03-15, 20, 18 yaş sınırı dahil",
            "2026-07-01, 2010-03-15, 20, 17 yaş",
            "2026-07-01, 2008-03-15, 14, 19 yaş: kural uygulanmaz",
            "2027-01-10, 1970-01-01, 0,  57 yaş ama 0 yıl: yaş kuralı hak doğurmaz"
    })
    void calculatesAnnualLeaveDaysBySeniorityAndAge(LocalDate hireDate, LocalDate birthDate,
                                                    int expected, String description) {
        assertThat(policy.annualLeaveDays(hireDate, birthDate, YEAR)).isEqualTo(expected);
    }

    @Test
    void rejectsYearBeforeHireYear() {
        LocalDate hireDate = LocalDate.of(2028, 1, 2);
        LocalDate birthDate = LocalDate.of(1995, 1, 1);

        assertThatThrownBy(() -> policy.annualLeaveDays(hireDate, birthDate, YEAR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2027")
                .hasMessageContaining("2028");
    }

    @Test
    void rejectsMissingDates() {
        LocalDate date = LocalDate.of(2020, 1, 1);

        assertThatThrownBy(() -> policy.annualLeaveDays(null, date, YEAR))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.annualLeaveDays(date, null, YEAR))
                .isInstanceOf(NullPointerException.class);
    }
}
