package com.dogus.vacation.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.dogus.vacation.domain.exception.InsufficientBalanceException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bir çalışanın bir izin türündeki yıllık bakiyesi ({@code leave_balances}).
 *
 * <p>Kalan gün saklanmaz, {@link #remainingDays()} ile hesaplanır. Gün alanlarının
 * setter'ı yoktur; kullanılan gün yalnızca {@link #use(BigDecimal)} ve
 * {@link #restore(BigDecimal)} ile, hak edilen gün yalnızca devirde
 * ({@link #transferOut(BigDecimal)}, {@link #transferIn(BigDecimal)}) değişir. Bu metotlar
 * bakiyenin negatife düşmemesini (BR-05) kendileri korur. Değişikliğin hareket kaydını
 * {@code LeaveBalanceService} yazar; entity'yi doğrudan değiştiren kod hareket
 * kaydını atlamış olur.
 */
@Entity
@Table(name = "leave_balances")
public class LeaveBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    /** Sütun {@code SMALLINT}; Java tarafında rahat kullanım için {@code Integer}. */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "entitled_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal entitledDays;

    @Column(name = "used_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedDays = BigDecimal.ZERO;

    /** İyimser kilit: aynı bakiyeye iki talep aynı anda düşemez (NFR-08). */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** JPA için; uygulama kodu kullanmaz. */
    protected LeaveBalance() {
    }

    public LeaveBalance(Employee employee, LeaveType leaveType, Integer year, BigDecimal entitledDays) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.year = year;
        this.entitledDays = entitledDays;
    }

    /** Kalan gün = hak edilen - kullanılan. */
    public BigDecimal remainingDays() {
        return entitledDays.subtract(usedDays);
    }

    /**
     * Talep edilen gün kalan bakiyeye sığmıyorsa hata verir; bakiyeyi değiştirmez.
     * Talep açılırken (FR-16) ve onayda {@link #use(BigDecimal)} içinde kullanılır.
     *
     * @param days talep edilen gün, sıfırdan büyük
     * @throws InsufficientBalanceException kalan gün yetmiyorsa (BR-05, 422)
     * @throws IllegalArgumentException     gün sıfır veya negatifse
     */
    public void requireAvailable(BigDecimal days) {
        requirePositive(days);
        BigDecimal remaining = remainingDays();
        if (remaining.compareTo(days) < 0) {
            throw new InsufficientBalanceException("Yetersiz izin bakiyesi: talep "
                    + plain(days) + " gün, kalan " + plain(remaining) + " gün.");
        }
    }

    /**
     * Bakiyeden gün kullanır (onay, T1).
     *
     * @param days kullanılacak gün, sıfırdan büyük
     * @throws InsufficientBalanceException kalan gün yetmiyorsa; bakiye değişmez
     * @throws IllegalArgumentException     gün sıfır veya negatifse
     */
    public void use(BigDecimal days) {
        requireAvailable(days);
        usedDays = usedDays.add(days);
    }

    /**
     * Kullanılmış günü bakiyeye geri verir (iptal, T2).
     *
     * @param days iade edilecek gün, sıfırdan büyük
     * @throws IllegalStateException    iade edilecek gün kullanılan günden fazlaysa; bu,
     *                                  daha önce düşülmemiş bir talebin iade edildiği
     *                                  anlamına gelir ve bir programlama hatasıdır
     * @throws IllegalArgumentException gün sıfır veya negatifse
     */
    public void restore(BigDecimal days) {
        requirePositive(days);
        if (usedDays.compareTo(days) < 0) {
            throw new IllegalStateException("İade edilecek gün (" + plain(days)
                    + ") kullanılan günden (" + plain(usedDays) + ") fazla olamaz.");
        }
        usedDays = usedDays.subtract(days);
    }

    /**
     * Kalan günün bir kısmını sonraki yıla devretmek için bu bakiyeden çıkarır (BR-12).
     *
     * <p>Hak edilen gün azalır, kullanılan gün değişmez: "kullanılan" gerçekten alınmış izni
     * göstermeye devam eder. Devredilen gün artık bu yılda kullanılamaz.
     *
     * @param days devredilecek gün, sıfırdan büyük
     * @throws InsufficientBalanceException kalan gün yetmiyorsa; bakiye değişmez
     * @throws IllegalArgumentException     gün sıfır veya negatifse
     */
    public void transferOut(BigDecimal days) {
        requireAvailable(days);
        entitledDays = entitledDays.subtract(days);
    }

    /**
     * Önceki yıldan devreden günü bu bakiyeye ekler (BR-12); hak edilen gün artar.
     *
     * @param days devreden gün, sıfırdan büyük
     * @throws IllegalArgumentException gün sıfır veya negatifse
     */
    public void transferIn(BigDecimal days) {
        requirePositive(days);
        entitledDays = entitledDays.add(days);
    }

    private static void requirePositive(BigDecimal days) {
        Objects.requireNonNull(days, "days");
        if (days.signum() <= 0) {
            throw new IllegalArgumentException("Gün sayısı sıfırdan büyük olmalı.");
        }
    }

    /** 5.0 → "5", 2.5 → "2.5": mesajda gereksiz ondalık görünmesin. */
    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public Integer getYear() {
        return year;
    }

    public BigDecimal getEntitledDays() {
        return entitledDays;
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }

    public Long getVersion() {
        return version;
    }
}
