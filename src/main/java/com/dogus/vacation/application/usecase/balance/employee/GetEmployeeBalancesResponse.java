package com.dogus.vacation.application.usecase.balance.employee;

import java.util.List;

import com.dogus.vacation.application.common.view.BalanceView;
import com.dogus.vacation.application.common.view.EmployeeRef;

/**
 * @param employee bakiyeleri gösterilen çalışan
 * @param year     bakiyelerin yılı
 * @param balances izin türü sırasıyla bakiyeler; o yıl için bakiye açılmamışsa boş
 */
public record GetEmployeeBalancesResponse(EmployeeRef employee, int year, List<BalanceView> balances) {

    public GetEmployeeBalancesResponse {
        balances = List.copyOf(balances);
    }
}
