package com.dogus.vacation.domain.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Yıllık hak edişin bir yıl için çalıştırıldığının kaydı ({@code annual_accrual_runs}).
 *
 * <p>BR-11 ("aynı yıl için iki kez çalıştırılamaz") bu kayıtla denetlenir;
 * {@code uq_annual_accrual_runs_year} yıl başına tek kayda izin verir. Sadece eklenir.
 */
@Entity
@Table(name = "annual_accrual_runs")
public class AnnualAccrualRun extends BaseEntity {

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "run_at", nullable = false)
    private OffsetDateTime runAt;

    /** Çalıştıran İK; sistem çalıştırdıysa boştur. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_by")
    private Employee runBy;

    @Column(name = "processed_count", nullable = false)
    private Integer processedCount;

    /** JPA için; uygulama kodu kullanmaz. */
    protected AnnualAccrualRun() {
    }

    public AnnualAccrualRun(Integer year, OffsetDateTime runAt, Employee runBy, Integer processedCount) {
        this.year = year;
        this.runAt = runAt;
        this.runBy = runBy;
        this.processedCount = processedCount;
    }

    public Integer getYear() {
        return year;
    }

    public OffsetDateTime getRunAt() {
        return runAt;
    }

    public Employee getRunBy() {
        return runBy;
    }

    public Integer getProcessedCount() {
        return processedCount;
    }
}
