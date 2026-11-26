package com.dogus.vacation.application.usecase.balance.mine;

import java.util.List;

import com.dogus.vacation.application.common.view.BalanceView;

/**
 * @param year     bakiyelerin yılı (istekte yıl yoksa bu yıl)
 * @param balances izin türü sırasıyla bakiyeler; o yıl için bakiye açılmamışsa boş
 */
public record GetMyBalancesResponse(int year, List<BalanceView> balances) {

    public GetMyBalancesResponse {
        balances = List.copyOf(balances);
    }
}
