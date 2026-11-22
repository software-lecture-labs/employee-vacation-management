package com.dogus.vacation.application.common.auth;

import com.dogus.vacation.domain.model.Employee;

/**
 * Giriş yapan çalışana erişim token'ı verir.
 *
 * <p>Use case katmanı token'ın biçimini (JWT, imza, süre) bilmez; bu arayüze bağlıdır.
 * REST'teki uygulaması {@code infrastructure.security.JwtTokenService}'tir; birim
 * testlerinde sahtesi verilir.
 */
public interface AccessTokenIssuer {

    /**
     * @param employee kimliği doğrulanmış, kaydedilmiş çalışan
     * @return token ve bitiş anı
     */
    IssuedToken issue(Employee employee);
}
