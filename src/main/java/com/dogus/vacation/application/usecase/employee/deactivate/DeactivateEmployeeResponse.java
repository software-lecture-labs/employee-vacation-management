package com.dogus.vacation.application.usecase.employee.deactivate;

import com.dogus.vacation.application.common.view.EmployeeView;

/**
 * @param employee çalışanın güncel hali ({@code active = false})
 */
public record DeactivateEmployeeResponse(EmployeeView employee) {
}
