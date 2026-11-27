package com.dogus.vacation.application.usecase.department.update;

import com.dogus.vacation.application.common.view.DepartmentView;

/**
 * @param department departmanın güncel hali
 */
public record UpdateDepartmentResponse(DepartmentView department) {
}
