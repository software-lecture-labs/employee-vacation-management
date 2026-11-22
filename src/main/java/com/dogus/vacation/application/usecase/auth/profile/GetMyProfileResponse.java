package com.dogus.vacation.application.usecase.auth.profile;

import com.dogus.vacation.application.common.view.EmployeeView;

/**
 * @param employee giriş yapan çalışan
 */
public record GetMyProfileResponse(EmployeeView employee) {
}
