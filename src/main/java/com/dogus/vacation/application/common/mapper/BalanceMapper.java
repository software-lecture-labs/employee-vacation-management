package com.dogus.vacation.application.common.mapper;

import com.dogus.vacation.application.common.view.BalanceView;
import com.dogus.vacation.application.common.view.LeaveTypeRef;
import com.dogus.vacation.domain.model.LeaveBalance;

/** {@link LeaveBalance} entity'sini {@link BalanceView}'a çevirir; kalan gün entity'de hesaplanır. */
public final class BalanceMapper {

    private BalanceMapper() {
    }

    public static BalanceView toView(LeaveBalance balance) {
        return new BalanceView(
                LeaveTypeRef.of(balance.getLeaveType()),
                balance.getYear(),
                balance.getEntitledDays(),
                balance.getUsedDays(),
                balance.remainingDays());
    }
}
