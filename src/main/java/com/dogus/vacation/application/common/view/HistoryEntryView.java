package com.dogus.vacation.application.common.view;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dogus.vacation.domain.model.enums.LeaveRequestStatus;

/**
 * Şirket izin geçmişinde bir olay: bir talebin bir durumdan diğerine geçişi (FR-26).
 *
 * @param id             geçmiş satırının id'si
 * @param occurredAt     geçişin zamanı
 * @param leaveRequestId talep id'si
 * @param employee       talep sahibi
 * @param departmentName talep sahibinin departmanı
 * @param leaveType      izin türü
 * @param startDate      izin başlangıcı
 * @param endDate        izin bitişi
 * @param fromStatus     önceki durum; talep açılışında boş
 * @param toStatus       yeni durum
 * @param actor          işlemi yapan; sistem işleminde (gece görevi) boş
 * @param note           gerekçe veya açıklama
 */
public record HistoryEntryView(
        Long id,
        OffsetDateTime occurredAt,
        Long leaveRequestId,
        EmployeeRef employee,
        String departmentName,
        LeaveTypeRef leaveType,
        LocalDate startDate,
        LocalDate endDate,
        LeaveRequestStatus fromStatus,
        LeaveRequestStatus toStatus,
        EmployeeRef actor,
        String note) {
}
