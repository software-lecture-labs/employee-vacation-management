package com.dogus.vacation.application.usecase.employee.get;

import com.dogus.vacation.application.common.view.EmployeeView;

/**
 * @param employee istenen çalışan; şifre hash'i ve version dönmez
 */
public record GetEmployeeResponse(EmployeeView employee) {
}
