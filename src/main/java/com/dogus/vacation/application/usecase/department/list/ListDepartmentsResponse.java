package com.dogus.vacation.application.usecase.department.list;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.DepartmentView;

/**
 * Departman listesinin cevabı (FR-10).
 *
 * @param departments departmanlar, sayfalı ve ada göre sıralı
 */
public record ListDepartmentsResponse(PageResponse<DepartmentView> departments) {
}
