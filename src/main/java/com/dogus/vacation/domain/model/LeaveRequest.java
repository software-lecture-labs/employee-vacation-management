package com.dogus.vacation.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * İzin talebi ({@code leave_requests}). Yeni talep {@link LeaveRequestStatus#PENDING} başlar.
 *
 * <p>Bu sınıf yalnızca alanları kurar. Durum geçiş metotları (onay, red, geri çekme,
 * iptal) ve geçersiz geçişte hata fırlatma ayrı görevdir (Plan 6.1, 18 Kasım);
 * bu yüzden durum ve karar alanlarının setter'ı yoktur.
 */
@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "working_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal workingDays;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    /** Kararı veren kişi (onay ya da red). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private Employee approver;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "cancel_request_reason", length = 500)
    private String cancelRequestReason;

    @Column(name = "cancel_requested_at")
    private OffsetDateTime cancelRequestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private Employee cancelledBy;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    /** İyimser kilit: aynı talep aynı anda iki kez onaylanamaz (NFR-08). */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** JPA için; uygulama kodu kullanmaz. */
    protected LeaveRequest() {
    }

    public LeaveRequest(Employee employee, LeaveType leaveType, LocalDate startDate,
                        LocalDate endDate, BigDecimal workingDays, String reason) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.workingDays = workingDays;
        this.reason = reason;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getWorkingDays() {
        return workingDays;
    }

    public String getReason() {
        return reason;
    }

    public LeaveRequestStatus getStatus() {
        return status;
    }

    public Employee getApprover() {
        return approver;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getCancelRequestReason() {
        return cancelRequestReason;
    }

    public OffsetDateTime getCancelRequestedAt() {
        return cancelRequestedAt;
    }

    public Employee getCancelledBy() {
        return cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Long getVersion() {
        return version;
    }
}
