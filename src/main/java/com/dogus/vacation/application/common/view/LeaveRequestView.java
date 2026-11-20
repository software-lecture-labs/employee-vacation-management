package com.dogus.vacation.application.common.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;

/**
 * Listelerde bir izin talebinin özeti ({@code GetMyLeaves}, onay bekleyenler).
 *
 * @param id          talep id'si
 * @param employee    talep sahibi
 * @param leaveType   izin türü
 * @param startDate   başlangıç (dahil)
 * @param endDate     bitiş (dahil)
 * @param workingDays iş günü sayısı
 * @param status      güncel durum
 */
public record LeaveRequestView(
        Long id,
        EmployeeRef employee,
        LeaveTypeRef leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal workingDays,
        LeaveRequestStatus status) {
}
