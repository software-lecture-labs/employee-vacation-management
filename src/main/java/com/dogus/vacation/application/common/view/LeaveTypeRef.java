package com.dogus.vacation.application.common.view;

import com.dogus.vacation.domain.model.LeaveType;

/**
 * Cevaplarda bir izin türünün kısa gösterimi.
 *
 * @param id   izin türü id'si
 * @param code değişmeyen kod ({@code ANNUAL}, {@code EXCUSE}, {@code SICK}, {@code UNPAID})
 * @param name görünen ad
 */
public record LeaveTypeRef(Long id, String code, String name) {

    /** @return izin türü boşsa {@code null} */
    public static LeaveTypeRef of(LeaveType leaveType) {
        if (leaveType == null) {
            return null;
        }
        return new LeaveTypeRef(leaveType.getId(), leaveType.getCode(), leaveType.getName());
    }
}
