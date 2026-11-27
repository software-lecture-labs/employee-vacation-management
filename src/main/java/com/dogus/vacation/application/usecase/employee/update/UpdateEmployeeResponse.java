package com.dogus.vacation.application.usecase.employee.update;

import com.dogus.vacation.application.common.view.EmployeeView;

/**
 * @param employee çalışanın güncel hali
 */
public record UpdateEmployeeResponse(EmployeeView employee) {
}
