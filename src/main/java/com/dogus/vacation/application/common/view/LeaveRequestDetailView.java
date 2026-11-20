package com.dogus.vacation.application.common.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;

/**
 * Bir izin talebinin bütün ayrıntıları ({@code SubmitLeave}, {@code GetLeaveDetail},
 * onay, red ve iptal cevapları). Çalışan, onaylayanı ve red/iptal gerekçesini buradan
 * görür.
 *
 * @param id                  talep id'si
 * @param employee            talep sahibi
 * @param leaveType           izin türü
 * @param startDate           başlangıç (dahil)
 * @param endDate             bitiş (dahil)
 * @param workingDays         iş günü sayısı
 * @param reason              talep açıklaması
 * @param status              güncel durum
 * @param approver            kararı veren (onay veya red); karar yoksa {@code null}
 * @param decisionNote        red gerekçesi
 * @param decidedAt           karar anı
 * @param cancelRequestReason çalışanın iptal talebi gerekçesi
 * @param cancelRequestedAt   iptal talebi anı
 * @param cancelledBy         iptal eden
 * @param cancelReason        iptal gerekçesi
 * @param cancelledAt         iptal anı
 */
public record LeaveRequestDetailView(
        Long id,
        EmployeeRef employee,
        LeaveTypeRef leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal workingDays,
        String reason,
        LeaveRequestStatus status,
        EmployeeRef approver,
        String decisionNote,
        OffsetDateTime decidedAt,
        String cancelRequestReason,
        OffsetDateTime cancelRequestedAt,
        EmployeeRef cancelledBy,
        String cancelReason,
        OffsetDateTime cancelledAt) {
}
