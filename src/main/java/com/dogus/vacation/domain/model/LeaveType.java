package com.dogus.vacation.domain.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * İzin türü ({@code leave_types}). İlk dört tür {@code 02_seed_leave_types.sql} ile gelir.
 *
 * <p>{@code code} değişmez (ANNUAL, EXCUSE, SICK, UNPAID): ad değişse bile kod aynı
 * kalır ve hak ediş yıllık izni koduyla bulur. Bu yüzden setter'ı yoktur ve sütun
 * {@code updatable = false} ile güncellemeye kapalıdır.
 */
@Entity
@Table(name = "leave_types")
public class LeaveType extends BaseEntity {

    @Column(name = "code", nullable = false, updatable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "deducts_from_balance", nullable = false)
    private boolean deductsFromBalance;

    /** Boş olabilir: yıllık iznin sınırı kıdem ve yaşa göre hesaplanır (FR-33). */
    @Column(name = "annual_limit_days", precision = 4, scale = 1)
    private BigDecimal annualLimitDays;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** JPA için; uygulama kodu kullanmaz. */
    protected LeaveType() {
    }

    public LeaveType(String code, String name, boolean deductsFromBalance, BigDecimal annualLimitDays) {
        this.code = code;
        this.name = name;
        this.deductsFromBalance = deductsFromBalance;
        this.annualLimitDays = annualLimitDays;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDeductsFromBalance() {
        return deductsFromBalance;
    }

    public void setDeductsFromBalance(boolean deductsFromBalance) {
        this.deductsFromBalance = deductsFromBalance;
    }

    public BigDecimal getAnnualLimitDays() {
        return annualLimitDays;
    }

    public void setAnnualLimitDays(BigDecimal annualLimitDays) {
        this.annualLimitDays = annualLimitDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
