package com.dogus.vacation.application.usecase.auth.login;

import com.dogus.vacation.application.mediator.Request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * E-posta ve şifreyle giriş (FR-01).
 *
 * <pre>{@code
 * { "email": "ayse.yilmaz@sirket.com", "password": "Sifre123!" }
 * }</pre>
 *
 * <p>Alanlar arası kural yoktur ({@link LoginValidator} boş liste döner); kullanıcının var
 * olup olmadığı, aktifliği ve şifresi veritabanına baktığı için Handler'da kontrol edilir.
 *
 * @param email    e-posta; büyük/küçük harf fark etmez
 * @param password şifre (düz metin, sadece istek boyunca; hiçbir yere yazılmaz)
 */
public record LoginRequest(
        @NotBlank(message = "E-posta zorunlu.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 120, message = "E-posta en fazla 120 karakter olabilir.")
        String email,

        @NotBlank(message = "Şifre zorunlu.")
        @Size(max = 72, message = "Şifre en fazla 72 karakter olabilir.")
        String password)
        implements Request<LoginResponse> {

    /** Şifre loglara düşmesin. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
