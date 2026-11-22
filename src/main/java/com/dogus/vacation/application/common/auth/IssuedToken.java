package com.dogus.vacation.application.common.auth;

import java.time.OffsetDateTime;

/**
 * Verilen erişim token'ı.
 *
 * @param value     {@code Authorization: Bearer <value>} başlığında gönderilecek token
 * @param expiresAt token'ın geçerliliğini yitirdiği an (Europe/Istanbul)
 */
public record IssuedToken(String value, OffsetDateTime expiresAt) {
}
