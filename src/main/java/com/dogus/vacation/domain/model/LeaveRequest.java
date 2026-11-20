package com.dogus.vacation.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.dogus.vacation.domain.exception.InvalidStateTransitionException;
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
 * <p>Durum ve karar alanlarının setter'ı yoktur; durum yalnızca aşağıdaki geçiş
 * metotlarıyla değişir. Geçersiz bir geçişte {@link InvalidStateTransitionException}
 * (409) atılır; böylece hangi servis çağırırsa çağırsın reddedilmiş bir talep onaylanamaz.
 *
 * <pre>
 * PENDING ──approve──▶ APPROVED ──requestCancellation──▶ CANCEL_REQUESTED
 *    │ reject ▶ REJECTED     │ cancel ▶ CANCELLED           │ approveCancellationRequest ▶ CANCELLED
 *    │ withdraw ▶ WITHDRAWN  │                              │ reject/withdrawCancellationRequest ▶ APPROVED
 *                                                           │ cancel ▶ CANCELLED
 *                                                           │ expireCancellationRequest ▶ CANCEL_REQUEST_EXPIRED
 * </pre>
 *
 * <p>Yetki (kim onaylayabilir, talep kimin) bu sınıfta değil, Handler'da ve
 * {@code ApproverResolver}'da kontrol edilir. Geçmiş kaydını {@code LeaveHistoryService} yazar.
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

    // ---- Durum geçişleri ----

    /**
     * Bekleyen talebi onaylar (FR-21). Bitiş tarihi geçmiş talep onaylanamaz, sadece
     * reddedilebilir (plan K-11).
     *
     * @param decidedBy onaylayan kişi
     * @param today     bugünün tarihi ({@code Clock}'tan)
     * @param now       karar anı
     */
    public void approve(Employee decidedBy, LocalDate today, OffsetDateTime now) {
        requireStatus(EnumSet.of(LeaveRequestStatus.PENDING), "onaylanamaz");
        if (today.isAfter(endDate)) {
            throw new InvalidStateTransitionException(
                    "Bitiş tarihi geçmiş bir talep onaylanamaz, sadece reddedilebilir.");
        }
        decide(LeaveRequestStatus.APPROVED, decidedBy, null, now);
    }

    /**
     * Bekleyen talebi gerekçeyle reddeder (FR-22, BR-09).
     *
     * @param decidedBy reddeden kişi
     * @param note      red gerekçesi; boş olamaz
     * @param now       karar anı
     */
    public void reject(Employee decidedBy, String note, OffsetDateTime now) {
        requireStatus(EnumSet.of(LeaveRequestStatus.PENDING), "reddedilemez");
        decide(LeaveRequestStatus.REJECTED, decidedBy, requireText(note, "Red gerekçesi"), now);
    }

    /** Bekleyen talebi geri çeker (FR-18, BR-14). Sahiplik kontrolü Handler'dadır. */
    public void withdraw() {
        requireStatus(EnumSet.of(LeaveRequestStatus.PENDING), "geri çekilemez");
        status = LeaveRequestStatus.WITHDRAWN;
    }

    /**
     * Onaylı izin için iptal talebi açar (FR-25). İzin başlamadan önce açılabilir (BR-08).
     *
     * @param cancelReason iptal gerekçesi; boş olamaz
     * @param today        bugünün tarihi
     * @param now          talep anı
     */
    public void requestCancellation(String cancelReason, LocalDate today, OffsetDateTime now) {
        requireStatus(EnumSet.of(LeaveRequestStatus.APPROVED), "için iptal talebi açılamaz");
        requireNotStarted(today, "Başlamış bir izin için iptal talebi açılamaz.");
        this.cancelRequestReason = requireText(cancelReason, "İptal gerekçesi");
        this.cancelRequestedAt = now;
        status = LeaveRequestStatus.CANCEL_REQUESTED;
    }

    /** Talep sahibi iptal talebinden vazgeçer; izin onaylı kalır (FR-29). */
    public void withdrawCancellationRequest() {
        requireStatus(EnumSet.of(LeaveRequestStatus.CANCEL_REQUESTED), "iptal talebinden vazgeçilemez");
        clearCancellationRequest();
        status = LeaveRequestStatus.APPROVED;
    }

    /** Onaylayıcı iptal talebini reddeder; izin onaylı kalır, günler iade edilmez (FR-28, BR-17). */
    public void rejectCancellationRequest() {
        requireStatus(EnumSet.of(LeaveRequestStatus.CANCEL_REQUESTED), "iptal talebi reddedilemez");
        clearCancellationRequest();
        status = LeaveRequestStatus.APPROVED;
    }

    /**
     * Onaylayıcı iptal talebini kabul eder; izin iptal olur (FR-28, BR-17). Günlerin
     * iadesi aynı transaction'da {@code LeaveBalanceService.refund} ile yapılır (T2).
     *
     * @param cancelledBy kabul eden kişi
     * @param today       bugünün tarihi; izin başladıysa kabul edilemez
     * @param now         iptal anı
     */
    public void approveCancellationRequest(Employee cancelledBy, LocalDate today, OffsetDateTime now) {
        requireStatus(EnumSet.of(LeaveRequestStatus.CANCEL_REQUESTED), "iptal talebi kabul edilemez");
        requireNotStarted(today, "İzin başladığı için iptal talebi kabul edilemez.");
        markCancelled(cancelledBy, cancelRequestReason, now);
    }

    /**
     * Yönetici veya İK onaylı izni şirket adına iptal eder (FR-23, BR-08, BR-09).
     * Bekleyen bir iptal talebi varsa o da kapanır.
     *
     * @param cancelledBy iptal eden kişi
     * @param reason      iptal gerekçesi; boş olamaz
     * @param today       bugünün tarihi; izin başladıysa iptal edilemez
     * @param now         iptal anı
     */
    public void cancel(Employee cancelledBy, String reason, LocalDate today, OffsetDateTime now) {
        requireStatus(EnumSet.of(LeaveRequestStatus.APPROVED, LeaveRequestStatus.CANCEL_REQUESTED),
                "iptal edilemez");
        requireNotStarted(today, "Başlamış bir izin iptal edilemez.");
        markCancelled(cancelledBy, requireText(reason, "İptal gerekçesi"), now);
    }

    /**
     * Başlangıç günü gelen ve karara bağlanmamış iptal talebini sonuçsuz yapar
     * (FR-31, BR-18; gece görevi T5). İzin onaylı olarak kullanılır.
     *
     * @param today bugünün tarihi; başlangıç gününden önce çağrılırsa hata
     */
    public void expireCancellationRequest(LocalDate today) {
        requireStatus(EnumSet.of(LeaveRequestStatus.CANCEL_REQUESTED), "iptal talebi sonuçsuz yapılamaz");
        if (today.isBefore(startDate)) {
            throw new InvalidStateTransitionException("İptal talebinin süresi henüz dolmadı.");
        }
        status = LeaveRequestStatus.CANCEL_REQUEST_EXPIRED;
    }

    /** @return talep bu çalışana aitse {@code true} (BR-14 ve FR-24 kontrolleri için) */
    public boolean isOwnedBy(Employee candidate) {
        return employee.equals(candidate);
    }

    private void requireStatus(Set<LeaveRequestStatus> allowed, String action) {
        if (!allowed.contains(status)) {
            throw new InvalidStateTransitionException(
                    "Talep '" + status.label() + "' durumundayken " + action + ".");
        }
    }

    private void requireNotStarted(LocalDate today, String message) {
        Objects.requireNonNull(today, "today");
        if (!today.isBefore(startDate)) {
            throw new InvalidStateTransitionException(message);
        }
    }

    private void decide(LeaveRequestStatus newStatus, Employee decidedBy, String note, OffsetDateTime now) {
        this.approver = Objects.requireNonNull(decidedBy, "decidedBy");
        this.decisionNote = note;
        this.decidedAt = Objects.requireNonNull(now, "now");
        this.status = newStatus;
    }

    private void markCancelled(Employee by, String reason, OffsetDateTime now) {
        this.cancelledBy = Objects.requireNonNull(by, "cancelledBy");
        this.cancelReason = reason;
        this.cancelledAt = Objects.requireNonNull(now, "now");
        this.status = LeaveRequestStatus.CANCELLED;
    }

    private void clearCancellationRequest() {
        this.cancelRequestReason = null;
        this.cancelRequestedAt = null;
    }

    private static String requireText(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(field + " boş olamaz.");
        }
        return text.strip();
    }

    // ---- Okuma ----

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
