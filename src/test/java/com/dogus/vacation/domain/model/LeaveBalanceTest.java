package com.dogus.vacation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.dogus.vacation.domain.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link LeaveBalance}'ın gün kurallarını veritabanı olmadan doğrular (BR-05).
 * Çalışan ve izin türü bu kurallarda kullanılmadığı için boş geçilir.
 */
class LeaveBalanceTest {

    private static LeaveBalance balanceOf(String entitledDays) {
        return new LeaveBalance(null, null, 2026, new BigDecimal(entitledDays));
    }

    @Test
    void newBalanceHasNoUsedDays() {
        LeaveBalance balance = balanceOf("14");

        assertThat(balance.getUsedDays()).isEqualByComparingTo("0");
        assertThat(balance.remainingDays()).isEqualByComparingTo("14");
    }

    @Test
    void useIncreasesUsedDaysAndDecreasesRemainingDays() {
        LeaveBalance balance = balanceOf("14");

        balance.use(new BigDecimal("3"));
        balance.use(new BigDecimal("2.5"));

        assertThat(balance.getUsedDays()).isEqualByComparingTo("5.5");
        assertThat(balance.remainingDays()).isEqualByComparingTo("8.5");
    }

    @Test
    void useCanSpendTheWholeBalance() {
        LeaveBalance balance = balanceOf("5");

        balance.use(new BigDecimal("5"));

        assertThat(balance.remainingDays()).isEqualByComparingTo("0");
    }

    @Test
    void useRejectsMoreDaysThanRemainingAndKeepsTheBalance() {
        LeaveBalance balance = balanceOf("5");
        balance.use(new BigDecimal("2"));

        assertThatThrownBy(() -> balance.use(new BigDecimal("5")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Yetersiz izin bakiyesi: talep 5 gün, kalan 3 gün.");
        assertThat(balance.getUsedDays()).isEqualByComparingTo("2");
    }

    @Test
    void requireAvailableDoesNotChangeTheBalance() {
        LeaveBalance balance = balanceOf("14");

        balance.requireAvailable(new BigDecimal("14"));

        assertThat(balance.getUsedDays()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> balance.requireAvailable(new BigDecimal("15")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void restoreGivesUsedDaysBack() {
        LeaveBalance balance = balanceOf("14");
        balance.use(new BigDecimal("5"));

        balance.restore(new BigDecimal("3"));

        assertThat(balance.getUsedDays()).isEqualByComparingTo("2");
        assertThat(balance.remainingDays()).isEqualByComparingTo("12");
    }

    @Test
    void restoreRejectsMoreDaysThanUsed() {
        LeaveBalance balance = balanceOf("14");
        balance.use(new BigDecimal("2"));

        assertThatThrownBy(() -> balance.restore(new BigDecimal("3")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(balance.getUsedDays()).isEqualByComparingTo("2");
    }

    @ParameterizedTest(name = "{0} gün")
    @ValueSource(strings = {"0", "-1"})
    void rejectsZeroOrNegativeDays(String days) {
        LeaveBalance balance = balanceOf("14");
        BigDecimal value = new BigDecimal(days);

        assertThatThrownBy(() -> balance.use(value)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> balance.restore(value)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> balance.requireAvailable(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingDays() {
        LeaveBalance balance = balanceOf("14");

        assertThatThrownBy(() -> balance.use(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> balance.restore(null)).isInstanceOf(NullPointerException.class);
    }

    // ---- devir (BR-12) ----

    @Test
    void transferOutLowersEntitledDaysAndKeepsUsedDays() {
        LeaveBalance balance = balanceOf("20");
        balance.use(new BigDecimal("8"));

        balance.transferOut(new BigDecimal("12"));

        assertThat(balance.getEntitledDays()).isEqualByComparingTo("8");
        assertThat(balance.getUsedDays()).isEqualByComparingTo("8");
        assertThat(balance.remainingDays()).isEqualByComparingTo("0");
    }

    @Test
    void transferOutRejectsMoreDaysThanRemainingAndKeepsTheBalance() {
        LeaveBalance balance = balanceOf("20");
        balance.use(new BigDecimal("8"));

        assertThatThrownBy(() -> balance.transferOut(new BigDecimal("13")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(balance.getEntitledDays()).isEqualByComparingTo("20");
    }

    @Test
    void transferInRaisesEntitledDays() {
        LeaveBalance balance = balanceOf("20");

        balance.transferIn(new BigDecimal("12.5"));

        assertThat(balance.getEntitledDays()).isEqualByComparingTo("32.5");
        assertThat(balance.remainingDays()).isEqualByComparingTo("32.5");
    }

    @ParameterizedTest(name = "{0} gün")
    @ValueSource(strings = {"0", "-1"})
    void transfersRejectZeroOrNegativeDays(String days) {
        LeaveBalance balance = balanceOf("14");
        BigDecimal value = new BigDecimal(days);

        assertThatThrownBy(() -> balance.transferOut(value)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> balance.transferIn(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
