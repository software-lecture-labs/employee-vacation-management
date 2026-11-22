package com.dogus.vacation.application.usecase.auth.login;

import java.time.OffsetDateTime;

import com.dogus.vacation.application.common.view.EmployeeView;

/**
 * Başarılı giriş. Sonraki isteklerde {@code Authorization: Bearer <accessToken>} başlığı gönderilir.
 *
 * @param accessToken JWT
 * @param tokenType   her zaman {@code Bearer}
 * @param expiresAt   token'ın geçerliliğini yitirdiği an
 * @param employee    giriş yapan çalışan
 */
public record LoginResponse(String accessToken, String tokenType, OffsetDateTime expiresAt, EmployeeView employee) {
}
