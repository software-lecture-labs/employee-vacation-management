package com.dogus.vacation.application.usecase.employee.create;

import java.util.List;

import com.dogus.vacation.application.common.view.BalanceView;
import com.dogus.vacation.application.common.view.EmployeeView;

/**
 * @param employee eklenen çalışan (şifre dönmez)
 * @param balances bu yıl için açılan ilk bakiyeler (T4); işe giriş gelecek yıldaysa boş
 */
public record CreateEmployeeResponse(EmployeeView employee, List<BalanceView> balances) {
}
