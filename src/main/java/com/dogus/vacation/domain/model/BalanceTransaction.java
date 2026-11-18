package com.dogus.vacation.domain.model;

import java.math.BigDecimal;

import com.dogus.vacation.domain.model.enums.BalanceTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bakiye hareketi ({@code balance_transactions}): kim, ne zaman, kaç gün, neden (FR-35).
 *
 * <p>Denetim kaydıdır; sadece eklenir, setter yoktur. Gün her zaman pozitiftir,
 * yönü {@link BalanceTransactionType} belirler ({@code ck_balance_transactions_days}).
 */
@Entity
@Table(name = "balance_transactions")
public class BalanceTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_balance_id", nullable = false)
    private LeaveBalance leaveBalance;

    /** Hak edişte boştur; düşüm ve iadede ilgili talep. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id")
    private LeaveRequest leaveRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private BalanceTransactionType type;

    @Column(name = "days", nullable = false, precision = 4, scale = 1)
    private BigDecimal days;

    @Column(name = "note", length = 500)
    private String note;

    /** İşlemi yapan; sistem işlemlerinde boştur. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Employee createdBy;

    /** JPA için; uygulama kodu kullanmaz. */
    protected BalanceTransaction() {
    }

    public BalanceTransaction(LeaveBalance leaveBalance, LeaveRequest leaveRequest,
                              BalanceTransactionType type, BigDecimal days, String note,
                              Employee createdBy) {
        this.leaveBalance = leaveBalance;
        this.leaveRequest = leaveRequest;
        this.type = type;
        this.days = days;
        this.note = note;
        this.createdBy = createdBy;
    }

    public LeaveBalance getLeaveBalance() {
        return leaveBalance;
    }

    public LeaveRequest getLeaveRequest() {
        return leaveRequest;
    }

    public BalanceTransactionType getType() {
        return type;
    }

    public BigDecimal getDays() {
        return days;
    }

    public String getNote() {
        return note;
    }

    public Employee getCreatedBy() {
        return createdBy;
    }
}
