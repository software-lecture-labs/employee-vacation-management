package com.dogus.vacation.application.usecase.department.create;

import com.dogus.vacation.application.common.view.DepartmentView;

/**
 * @param department eklenen departman
 */
public record CreateDepartmentResponse(DepartmentView department) {
}
