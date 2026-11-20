package com.dogus.vacation.application.common.mapper;

import com.dogus.vacation.application.common.view.LeaveTypeView;
import com.dogus.vacation.domain.model.LeaveType;

/**
 * {@link LeaveType} entity'sini dışarı verilen {@link LeaveTypeView}'a çevirir.
 * Entity Controller'dan dışarı çıkmaz; cevaplar yalnızca view taşır.
 */
public final class LeaveTypeMapper {

    private LeaveTypeMapper() {
    }

    public static LeaveTypeView toView(LeaveType leaveType) {
        return new LeaveTypeView(
                leaveType.getId(),
                leaveType.getCode(),
                leaveType.getName(),
                leaveType.isDeductsFromBalance(),
                leaveType.getAnnualLimitDays(),
                leaveType.isActive());
    }
}
