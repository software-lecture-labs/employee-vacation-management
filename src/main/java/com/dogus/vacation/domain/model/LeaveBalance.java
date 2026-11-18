package com.dogus.vacation.domain.model;

import java.math.BigDecimal;

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
 * <p>Kalan gün saklanmaz, {@link #remainingDays()} ile hesaplanır. Bakiyenin
 * değişmesi (düşüm, iade) ve hareket kaydı {@code LeaveBalanceService}'in işidir;
 * bu yüzden gün alanlarının setter'ı yoktur.
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
