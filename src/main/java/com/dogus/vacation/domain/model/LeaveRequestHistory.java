package com.dogus.vacation.domain.model;

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

/**
 * Talebin bir durum değişikliği ({@code leave_request_history}); İK'nın şirket izin
 * geçmişi ekranının kaynağı (FR-26, FR-27).
 *
 * <p>Sadece eklenir, güncellenmez: tüm alanlar constructor ile verilir, setter yoktur.
 */
@Entity
@Table(name = "leave_request_history")
public class LeaveRequestHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_request_id", nullable = false)
    private LeaveRequest leaveRequest;

    /** İlk kayıtta (talep açılırken) boştur. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private LeaveRequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private LeaveRequestStatus toStatus;

    /** İşlemi yapan; gece görevinde boştur ve API'de "Sistem" olarak gösterilir. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Employee actor;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    /** JPA için; uygulama kodu kullanmaz. */
    protected LeaveRequestHistory() {
    }

    public LeaveRequestHistory(LeaveRequest leaveRequest, LeaveRequestStatus fromStatus,
                               LeaveRequestStatus toStatus, Employee actor, String note,
                               OffsetDateTime occurredAt) {
        this.leaveRequest = leaveRequest;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    public LeaveRequest getLeaveRequest() {
        return leaveRequest;
    }

    public LeaveRequestStatus getFromStatus() {
        return fromStatus;
    }

    public LeaveRequestStatus getToStatus() {
        return toStatus;
    }

    public Employee getActor() {
        return actor;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
