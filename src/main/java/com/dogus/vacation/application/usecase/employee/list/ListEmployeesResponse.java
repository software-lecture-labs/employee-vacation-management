package com.dogus.vacation.application.usecase.employee.list;

import com.dogus.vacation.application.common.PageResponse;
import com.dogus.vacation.application.common.view.EmployeeView;

/**
 * Çalışan listesinin cevabı (FR-09).
 *
 * @param employees çalışanlar, sayfalı; soyada, sonra ada göre sıralı
 */
public record ListEmployeesResponse(PageResponse<EmployeeView> employees) {
}
